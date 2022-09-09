Feature: Ansible Action tests

  @ansibleaction
  Scenario: Job template is correctly run
    Given authenticate against Manager
    And Job template Greetings exists
    And create a new Bridge "mybridge" in cloud provider "aws" and region "us-east-1"
    And the Bridge "mybridge" is existing with status "ready" within 4 minutes
    And the Ingress of Bridge "mybridge" is available within 2 minutes
    And add a Processor to the Bridge "mybridge" with body:
    """
    {
      "name": "ansibleProcessor",
      "action": {
        "parameters": {
          "endpoint":  "${env.ansible.endpoint}",
          "job_template_id":      "${data.ansible.job.template.id}",
          "basic_auth_username": "${env.ansible.username}",
          "basic_auth_password": "${env.ansible.password}",
          "ssl_verification_disabled": ${env.ansible.ssl.verification.disabled}
        },
        "type": "ansible_tower_job_template_sink_0.1"
      },
      "transformationTemplate": "{\"extra_vars\":{\"name_to_greet\": \"{data.name}\"}}"
    }
    """
    And the list of Processor instances of the Bridge "mybridge" is containing the Processor "ansibleProcessor"
    And the Processor "ansibleProcessor" of the Bridge "mybridge" is existing with status "ready" within 3 minutes
    When send a cloud event to the Ingress of the Bridge "mybridge":
    """
    {
      "specversion": "1.0",
      "type": "ansible.event",
      "source": "AnsibleActionTestService",
      "id": "ansible-test",
      "data": {
        "name": "${uuid.name.to.greet}"
      }
    }
    """
    Then Ansible job with the extra parameter "${uuid.name.to.greet}" was run in 2 days using template "Greetings" within 2 minutes
