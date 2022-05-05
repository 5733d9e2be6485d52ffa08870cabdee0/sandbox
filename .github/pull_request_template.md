[Insert JIRA Issue number and update link](https://issues.redhat.com/browse/MGDOBR-)

## PR owner checklist

Please make sure that your PR meets the following requirements:

- [ ] Your code is properly formatted according to [this configuration](https://github.com/kiegroup/kogito-runtimes/tree/main/kogito-build/kogito-ide-config).  
  *Run `mvn clean verify -DskipTests` so that imports are correctly set.*
- [ ] Your commit messages are clear and reference the JIRA issue e.g: "[MGDOBR-1] - $clear_explanation_of_what_you_did"
- [ ] The layers in the `kustomize` folder have been updated accordingly.
- [ ] All new functionality is tested
- [ ] Pull Request title is properly formatted: `MGDOBR-XYZ Subject`
- [ ] Pull Request contains link to the JIRA issue
- [ ] Pull Request contains link to any dependent or related Pull Request
- [ ] Pull Request contains description of the issue
- [ ] Pull Request does not include fixes for issues other than the main ticket

## PR reviewer(s) checklist

- [ ] Check if code and Github action workflows have been modified and if the modification is safe
- [ ] If the modification is safe, add the `safe to test` label

<details>
<summary>
How to trigger pipelines and use the bots:
</summary>

- <b>Run the end to end pipeline</b>  
  Annotate the pull request with the label: `safe to test`. If you want to run the pipeline again, remove and add it again.

- <b>Rebase the pull request</b>  
  Comment with: `/rebase`.
- <b>Deploy BOT</b>
  Comment with `/deploy <dev,stable> when the PR has been merged to deploy to a target environment.

</details>
