# Report Portal integration

[Report Portal](https://reportportal.io/) is a tool for reporting of test results. To send the results to your
Report Portal instance you need to provide the following parameters. You can find values for most of them in
your Report Portal -> User profile -> Configuration examples.

| Parameter | Default value | Description |
| ----------- | ----------- | ----------- |
| rp.endpoint | http://127.0.0.1:8080 | URL of web service, where requests should be send |
| rp.uuid | 8c5b8aae-48ea-4984-a229-4ca8c7bfc0e9 | UUID of user. |
| rp.launch | sandbox | The unique name of Launch (Run). Based on that name a history of runs will be created for particular name |
| rp.project | default_personal | Project name to identify scope |
| rp.attributes | type:dummy | Set of tags for specifying additional meta information for current launch. Format: tag1;tag2;build:12345-6. |
| rp.enable | false | Enable/Disable sending data to Report Portal |

## Example

mvn clean install -Drp.endpoint=http://127.0.0.1:8080 -Drp.enable=true
