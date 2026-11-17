import unittest
from scan import assess


def report(risk='2', plugin='10020'):
    return {'site': [{'@host': 'mc2-front', '@port': '6080', 'alerts': [
        {'pluginid': plugin, 'riskcode': risk, 'name': 'synthetic', 'instances': []}]}]}


def report_with_ref(ref, plugin='10020', risk='2'):
    data = report(risk, plugin)
    data['site'][0]['alerts'][0]['alertRef'] = ref
    return data


def policy(risk=2):
    return {'decisions': {'front:10020': {'riskcode': risk, 'decision': 'risk_accepted',
            'reason': 'Synthetic justification with sufficient length for the decision record.',
            'reference': 'seguridad.md#f-test', 'requires_loopback': True}}}


class AssessmentTest(unittest.TestCase):
    def test_unrecorded_finding_blocks(self):
        self.assertEqual(['10020'], assess(report(), 'front', 2, {}, True)['blocked'])
    def test_documented_conditional_acceptance(self):
        self.assertEqual([], assess(report(), 'front', 2, policy(), True)['blocked'])
    def test_exposure_change_invalidates_acceptance(self):
        self.assertEqual(['10020'], assess(report(), 'front', 2, policy(), False)['blocked'])
    def test_high_always_blocks_even_if_recorded(self):
        self.assertEqual(['10020'], assess(report('3'), 'front', 2, policy(3), True)['blocked'])
    def test_severity_change_invalidates_record(self):
        self.assertEqual(['10020'], assess(report('1'), 'front', 2, policy(), True)['blocked'])
    def test_missing_and_wrong_origins_block(self):
        for data in ({}, {'site': []}, {'site': [{'@host': 'other', '@port': '6080', 'alerts': []}]}):
            with self.assertRaises(ValueError): assess(data, 'front', 0, {}, True)
    def test_scanner_errors_and_fail_are_not_waived(self):
        for code in (1, 3, 137):
            with self.assertRaises(ValueError): assess(report(), 'front', code, policy(), True)
    def test_missing_or_non_numeric_risk_is_rejected(self):
        for risk in ('', 'HIGH', '-1', '4'):
            with self.assertRaises(ValueError): assess(report(risk), 'front', 2, policy(), True)
    def test_valid_empty_site_passes(self):
        self.assertEqual([], assess({'site': [{'@host': 'mc2-front', '@port': '6080', 'alerts': []}]},
                                    'front', 0, {}, True)['blocked'])
    def test_new_plugin_is_not_covered_by_other_acceptance(self):
        self.assertEqual(['99999'], assess(report(plugin='99999'), 'front', 2, policy(), True)['blocked'])

    def test_policy_uses_alert_ref_instead_of_plugin_id(self):
        specific = {'decisions': {'front:10020-1': {
            'riskcode': 2, 'decision': 'risk_accepted',
            'reason': 'Synthetic justification with sufficient length for the decision record.',
            'reference': 'seguridad.md#f-test', 'requires_loopback': True}}}
        self.assertEqual([], assess(report_with_ref('10020-1'), 'front', 2, specific, True)['blocked'])
        self.assertEqual(['10020-2'], assess(report_with_ref('10020-2'), 'front', 2, specific, True)['blocked'])

    def test_http_exposure_outside_loopback_blocks_accepted_finding(self):
        self.assertEqual(['10020'], assess(report(), 'front', 2, policy(), False)['blocked'])

    def test_policy_cannot_accept_high_even_with_alert_ref(self):
        high_policy = policy(3)
        data = report_with_ref('10020-1', risk='3')
        high_policy['decisions']['front:10020-1'] = high_policy['decisions'].pop('front:10020')
        self.assertEqual(['10020-1'], assess(data, 'front', 2, high_policy, True)['blocked'])

    def test_malformed_site_and_alert_entries_are_rejected(self):
        malformed = [
            {'site': [None]},
            {'site': [{'@host': 'mc2-front', '@port': '6080', 'alerts': [None]}]},
            {'site': [{'@host': 'mc2-front', '@port': '6080', 'alerts': 'not-an-array'}]},
        ]
        for data in malformed:
            with self.subTest(data=data), self.assertRaises(ValueError):
                assess(data, 'front', 0, {}, True)

    def test_malformed_instances_are_rejected(self):
        for instances in (None, 'not-an-array', [None]):
            data = report()
            data['site'][0]['alerts'][0]['instances'] = instances
            with self.subTest(instances=instances), self.assertRaises(ValueError):
                assess(data, 'front', 2, {}, True)

    def test_non_boolean_loopback_policy_is_rejected(self):
        malformed_policy = policy()
        malformed_policy['decisions']['front:10020']['requires_loopback'] = []
        self.assertEqual(['10020'], assess(report(), 'front', 2, malformed_policy, False)['blocked'])


if __name__ == '__main__': unittest.main()
