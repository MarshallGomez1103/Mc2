#!/usr/bin/env python3
"""Baseline HTTP de los dos servicios Compose; sin escaneo activo ni análisis de TCP7777."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import uuid
from urllib.parse import urlsplit

# Imagen oficial ZAP2.17.0 verificada; digest evita tags mensuales móviles.
IMAGE = 'ghcr.io/zaproxy/zaproxy@sha256:9624669db72f54d86c4b46a7064518bd9bf79d8c61a4c31befa9923489f328af'
TARGETS = {'server': 'http://mc2-server:8080/health', 'front': 'http://mc2-front:6080/vnc.html'}


def assess(report, label, raw_exit, policy, loopback):
    """Un reporte inválido/inesperado o un hallazgo sin tratamiento bloquea el resultado."""
    if raw_exit not in (0, 2):
        raise ValueError(f'{label}: baseline error/FAIL, exit={raw_exit}')
    if not isinstance(report, dict): raise ValueError(f'{label}: report must be an object')
    sites = report.get('site')
    if not isinstance(sites, list) or not sites:
        raise ValueError(f'{label}: report without sites')
    if not isinstance(policy, dict) or not isinstance(policy.get('decisions', {}), dict):
        raise ValueError('Invalid decision policy')
    expected = urlsplit(TARGETS[label]); records = []; blocked = []
    for site in sites:
        if not isinstance(site, dict): raise ValueError(f'{label}: invalid site object')
        if site.get('@host') != expected.hostname or str(site.get('@port')) != str(expected.port):
            raise ValueError(f'{label}: unexpected report origin')
        alerts = site.get('alerts')
        if not isinstance(alerts, list):
            raise ValueError(f'{label}: missing alerts array')
        for alert in alerts:
            if not isinstance(alert, dict): raise ValueError(f'{label}: invalid finding object')
            plugin = str(alert.get('pluginid', ''))
            risk_value = str(alert.get('riskcode', ''))
            if not plugin.isdigit() or risk_value not in ('0', '1', '2', '3'):
                raise ValueError(f'{label}: invalid finding severity/id')
            ref = str(alert.get('alertRef', plugin))
            if not re.fullmatch(r'[0-9]+(?:-[0-9]+)?', ref): raise ValueError('Invalid alert reference')
            risk = int(risk_value); entry = policy.get('decisions', {}).get(label + ':' + ref)
            treated = (isinstance(entry, dict) and type(entry.get('riskcode')) is int and entry['riskcode'] == risk
                       and entry.get('decision') in ('risk_accepted', 'false_positive')
                       and isinstance(entry.get('reason'), str) and len(entry['reason'].strip()) >= 40
                       and isinstance(entry.get('reference'), str) and bool(entry['reference'].strip())
                       and isinstance(entry.get('requires_loopback', False), bool)
                       and (not entry.get('requires_loopback', False) or loopback))
            # A high finding is always blocking; the policy cannot waive it.
            if risk >= 3 or not treated:
                blocked.append(ref)
            instances = alert.get('instances', [])
            if not isinstance(instances, list) or any(not isinstance(i, dict) or not isinstance(i.get('uri'), str) for i in instances):
                raise ValueError(f'{label}: invalid finding instances')
            records.append({'target': label, 'pluginid': plugin, 'alertRef': ref, 'risk': risk,
                            'name': alert.get('name', alert.get('alert', '')),
                            'decision': entry.get('decision') if treated and risk < 3 else 'unresolved',
                            'instances': [i['uri'] for i in instances]})
    if raw_exit == 2 and not records: raise ValueError(f'{label}: WARN exit without reported alerts')
    return {'target': label, 'raw_exit': raw_exit, 'blocked': blocked, 'findings': records}


def docker(*args, timeout=60):
    return subprocess.run(['docker', *args], check=True, capture_output=True, text=True,
                          encoding='utf-8', timeout=timeout).stdout


def container(project, service):
    ids = docker('compose', '-p', project, 'ps', '-q', service).split()
    if len(ids) != 1:
        raise ValueError(f'{service}: expected one running container')
    value = json.loads(docker('inspect', ids[0]))[0]
    labels = value['Config'].get('Labels', {})
    if labels.get('com.docker.compose.project') != project or labels.get('com.docker.compose.service') != service:
        raise ValueError('Container identity does not match Compose project/service')
    if value['State'].get('Health', {}).get('Status') != 'healthy':
        raise ValueError(f'{service}: not healthy; execute after readiness')
    return value


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project', default=os.getenv('COMPOSE_PROJECT_NAME', 'mc2'))
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--revision', required=True)
    parser.add_argument('--policy', type=Path, default=Path(__file__).with_name('policy.json'))
    args = parser.parse_args(); out = args.out.resolve(); out.mkdir(parents=True, exist_ok=True)
    if os.name != 'nt': out.chmod(0o777)  # only this artifact directory, so image user zap can write
    verdict = {'success': False, 'revision': args.revision, 'results': []}
    (out/'verdict.json').write_text(json.dumps(verdict), encoding='utf-8')
    try:
        if not re.fullmatch(r'[0-9a-f]{40}', args.revision): raise ValueError('Revision must be a full commit SHA')
        policy = json.loads(args.policy.read_text(encoding='utf-8'))
        if not isinstance(policy, dict) or policy.get('schema') != 1: raise ValueError('Unsupported policy schema')
        server = container(args.project, 'mc2-server'); front = container(args.project, 'mc2-front')
        shared = set(server['NetworkSettings']['Networks']) & set(front['NetworkSettings']['Networks'])
        if len(shared) != 1: raise ValueError('Expected exactly one shared Compose network')
        network = shared.pop()
        loopback = {}
        for label, value, port in [('server', server, '8080/tcp'), ('front', front, '6080/tcp')]:
            bindings = value['HostConfig'].get('PortBindings', {}).get(port) or []
            loopback[label] = all(p.get('HostIp') in ('127.0.0.1', '::1') for p in bindings)
        image = json.loads(docker('image', 'inspect', IMAGE))[0]
        manifest = {'revision': args.revision, 'project': args.project, 'network': network,
                    'script_sha256': hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                    'policy_sha256': hashlib.sha256(args.policy.read_bytes()).hexdigest(),
                    'scanner': IMAGE, 'scanner_id': image['Id'], 'scanner_digests': image.get('RepoDigests', []),
                    'http_loopback_only': loopback, 'targets': TARGETS,
                    'containers': {label: {'id': c['Id'], 'image_id': c['Image'],
                                          'image': c['Config']['Image']} for label, c in [('server', server), ('front', front)]}}
        (out/'manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
        for label, url in TARGETS.items():
            for extension in ('json', 'html', 'xml', 'md', 'log'):
                (out/(label+'.'+extension)).unlink(missing_ok=True)
            # No -I/IGNORE rules: preserve default WARN classification and apply documented treatment below.
            name = args.project + '-zap-' + label + '-' + uuid.uuid4().hex[:10]
            command = ['docker', 'run', '--rm', '--name', name, '--network', network, '--cpus', '2', '--memory', '2g',
                       '-v', str(out) + ':/zap/wrk:rw', IMAGE, 'zap-baseline.py', '--autooff',
                       '-t', url, '-m', '1', '-T', '5', '-J', label+'.json', '-r', label+'.html',
                       '-x', label+'.xml', '-w', label+'.md',
                       '-z', '-config autoupdate.checkOnStart=false -config autoupdate.installAddonUpdates=false']
            try:
                with (out/(label+'.log')).open('w', encoding='utf-8') as log:
                    run = subprocess.run(command, stdout=log, stderr=subprocess.STDOUT, timeout=480)
            finally:
                # --rm covers normal completion; also remove our unique container after a timeout.
                subprocess.run(['docker', 'rm', '-f', name], stdout=subprocess.DEVNULL,
                               stderr=subprocess.DEVNULL, timeout=30)
            verdict['results'].append(assess(json.loads((out/(label+'.json')).read_text(encoding='utf-8')),
                                              label, run.returncode, policy, loopback[label]))
        verdict['success'] = not any(result['blocked'] for result in verdict['results'])
    except (ValueError, OSError, subprocess.SubprocessError, KeyError, TypeError) as exc:
        verdict['error'] = str(exc)
    finally:
        (out/'verdict.json').write_text(json.dumps(verdict, indent=2, ensure_ascii=False), encoding='utf-8')
        print(json.dumps(verdict, ensure_ascii=False))
    return 0 if verdict['success'] else 1


if __name__ == '__main__': sys.exit(main())
