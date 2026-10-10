# Atribuye muestras de CPU de JFR (salida de `jfr print --events jdk.ExecutionSample --stack-depth 64`)
# a componentes de la IA, dentro de una ventana de tiempo [from, to). Requiere gawk.
# Uso: gawk -v from=HH:MM:SS -v to=HH:MM:SS -f atribucion.awk samples.txt
function flush(){ if(t=="")return; if(t>=from && t<to){ total++; if(st ~ /EnemyUpdateService\.update\(/){ upd++;
  if(st ~ /AStarPathfinder\.findPath/) c["A* (findPath, incl.)"]++;
  else if(st ~ /ZombieSeparation\.resolve/) c["ZombieSeparation.resolve"]++;
  else if(st ~ /ZombieSeparation\.isOccupied/) c["ZombieSeparation.isOccupied"]++;
  else if(st ~ /resolveFallingContact/) c["resolveFallingContact"]++;
  else if(st ~ /ZombieMovement/) c["ZombieMovement (sin A*)"]++;
  else c["otro en update"]++; } else c["fuera de update (harness/JVM)"]++; top[leaf]++ } t="";st="";leaf="" }
/startTime =/ { flush(); t=$3; inst=0 }
/stackTrace = \[/ { inst=1; first=1; next }
inst && /line:/ { st=st"|"$0; if(first){leaf=$1; first=0} }
/^\]|^  \]/ { inst=0 }
END{ flush(); printf "ventana %s-%s: muestras %d, en update %d\n",from,to,total,upd; for(k in c) printf "  %-34s %6d  %5.1f%% de update\n",k,c[k],(k ~ /fuera/)?0:100*c[k]/upd; print "  top hojas:"; n=0; PROCINFO["sorted_in"]="@val_num_desc"; for(k in top){ if(n++<8) printf "    %6d %s\n",top[k],k } }
