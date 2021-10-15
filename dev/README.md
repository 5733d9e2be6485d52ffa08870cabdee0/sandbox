# DEV 

We provide a `docker-compose.yaml` file that you can use to spin up all the resources that the manager needs to run (keycloak, postgres, prometheus and grafana). 

To start it, run from the current directory

```bash
docker-compose up
```

If you want to generate some traffic automatically, we provide the script `generate_traffic.py` that you can run with 

```bash
python3 generate_traffic.py --manager=http://localhost:8080 --keycloak=http://localhost:8180 --username=kermit --password=thefrog --bad_request_rate=0.2 --match_filter_rate=0.2
```

The script runs forever, press `CTRL+C` to stop it.

With the parameters `--manager`, `--keycloak`, `--username` and `--password` you can configure the script so to target any environment (for example the demo environment).