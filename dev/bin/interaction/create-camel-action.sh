#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
action_name=$TODAY_ACTION_NAME
action_type='camel-new'

action_payload='{
  "name": '"\"$action_name\""',
    "flow": [
    {
       "from": {
          "uri": "rhose",
          "steps": [
            {
              "unmarshal": {
                "json": {}
              }
            },
            {
              "choice": {
                "when": [
                  {
                    "simple": "${body[nutritions][sugar]} <= 5",
                    "steps": [
                      {
                        "log": { "message" : "++++- Lesser equal than 5 ${body}" }
                      },
                      {
                        "marshal": {
                          "json": {}
                        }
                      },
                    {
                        "to": {
                            "uri": "rhoc:slack_sink_0.1:slackAction1",
                            "parameters": {
                                "slack_webhook_url": '"\"$SLACK_WEBHOOK_URL\""',
                                "slack_channel": "mc"
                            }
                        }
                    }
                    ]
                  },
                  {
                    "simple": "${body[nutritions][sugar]} > 5 && ${body[nutritions][sugar]} <= 10",
                    "steps": [
                      {
                        "log": { "message" : "++++- between 5 and 10 goes to http webhook ${body}" }
                      },
                      {
                        "marshal": {
                          "json": {}
                        }
                      },
                {
                        "to": {
                            "uri": "rhoc:http_sink_0.1:httpAction",
                            "parameters": {
                                "http_url": '"\"$HTTP_WEBHOOK_ACTION\""'
                            }
                        }
                    }
                    ]
                  }
                ],
                "otherwise": {
                  "steps": [
                    {
                      "marshal": {
                        "json": {}
                      }
                    },
                {
                        "to": {
                            "uri": "rhoc:slack_sink_0.1:errorAction",
                            "parameters": {
                                "slack_webhook_url": '"\"$SLACK_WEBHOOK_URL2\""',
                                "slack_channel": "mc2"
                            }
                        }
                    }
                  ]
                }
              }
            }
          ]
          }
        }
      ]
    }
}'

echo $action_payload
echo $action_payload | jq .

printf "\n\nCreating the Camel ${action_type} action with name $action_name\n"

# PROCESSOR_ID=$(curl -s -X POST -H "Authorization: $OB_TOKEN" -H 'Accept: application/json' -H 'Content-Type: application/json' -d "$action_payload" $MANAGER_URL/api/smartevents_mgmt/v1/bridges/$BRIDGE_ID/processors | jq -r .id)
# printf "\n\nAction ${action_type} created: $action_name\n"
# echo "export PROCESSOR_ID=$PROCESSOR_ID"


curl -v -X POST -H "Authorization: $OB_TOKEN" -H 'Accept: application/json' -H 'Content-Type: application/json' -d "$action_payload" $MANAGER_URL/api/smartevents_mgmt/v1/bridges/$BRIDGE_ID/camelProcessors
