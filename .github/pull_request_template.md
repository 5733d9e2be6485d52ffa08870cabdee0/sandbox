[Insert JIRA Issue number and update link](https://issues.redhat.com/browse/MGDOBR-) 

Please make sure that your PR meets the following requirements:

- [ ] Your code is properly formatted according to [this configuration](https://github.com/kiegroup/kogito-runtimes/tree/main/kogito-build/kogito-ide-config)
- [ ] Your commit messages are clear and reference the JIRA issue e.g: "[MGDOBR-1] - $clear_explanation_of_what_you_did"
- [ ] All new functionality is tested
- [ ] Pull Request title is properly formatted: `MGDOBR-XYZ Subject`
- [ ] Pull Request contains link to the JIRA issue
- [ ] Pull Request contains link to any dependent or related Pull Request
- [ ] Pull Request contains description of the issue
- [ ] Pull Request does not include fixes for issues other than the main ticket

<details>
<summary>
How to trigger pipelines and use the bots:
</summary>

* <b>Run the end to end pipeline</b>  
  Annotate the pull request with the label: `safe to test`.

* <b>Rebase the pull request</b>  
  Comment with: `/rebase`.

</details>
