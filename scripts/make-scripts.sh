#!/bin/bash

echo -e "#!/bin/bash\n\nexec java -jar bin/main.jar \"conf/server.conf\" \"INIT\" \"\$@\"\n" > out/run-init.sh
echo -e "#!/bin/bash\n\nexec java -jar bin/main.jar \"conf/server.conf\" \"conf/redemption-items.json\" \"GENESIS\" \"\$@\"\n" > out/run-genesis.sh
echo -e "#!/bin/bash\n\nexec java -jar bin/main.jar \"conf/server.conf\" \"conf/redemption-items.json\" \"SERVER\" \"\$@\"\n" > out/run.sh

chmod 770 out/*.sh

