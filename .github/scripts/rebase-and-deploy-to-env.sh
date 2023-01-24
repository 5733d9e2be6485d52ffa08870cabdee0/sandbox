# Env vars parameters:
# TARGET_BRANCH: dev, prod
# SHA_COMMIT: the sha of the commit to rebase
# AUTHOR: The user
# GITHUB_TOKEN: The token

allowed_branch_values=("dev" "prod")

# Check for valid choices
if ! grep -q "$TARGET_BRANCH" <<< "${allowed_branch_values[@]}"; then
    printf "\U274C $TARGET_BRANCH is not a valid choice. Valid choices are dev and prod."
    exit 0
fi

UPSTREAM_REPO_LOCATION=/tmp/upstream
git clone https://$AUTHOR:$GITHUB_TOKEN@github.com/5733d9e2be6485d52ffa08870cabdee0/sandbox.git $UPSTREAM_REPO_LOCATION > /dev/null 2>&1

cd $UPSTREAM_REPO_LOCATION
# peek branches 
git checkout --track origin/dev 1>&2
git checkout --track origin/prod 1>&2
git checkout main 1>&2


# If the deployment targets `prod`, then the feature must be on dev first.
if [[ "$TARGET_BRANCH" == "prod" ]]; then
  if [ $(git branch --contains $SHA_COMMIT | grep -c "dev") -eq 0 ]; then
    printf "\U274C In order to deploy to prod branch the feature must be on dev branch first. Please deploy the feature to dev, check that everything is running fine and then, finally, promote it to prod."
    exit 0
  fi
fi


# If the commit is already on TARGET_BRANCH, the feature has been already merged and deployed.
if [ $(git branch --contains $SHA_COMMIT | grep -c "$TARGET_BRANCH") -ne 0 ]; then
  printf "\U2705 $SHA_COMMIT is already on $TARGET_BRANCH branch!"
else
  git checkout $TARGET_BRANCH 1>&2
  git rebase $SHA_COMMIT 1>&2
  git push origin $TARGET_BRANCH 1>&2
  printf "\U2705 $SHA_COMMIT was not on $TARGET_BRANCH branch. $TARGET_BRANCH branch has been rebased and pushed to the upstream repository. Check ArgoCD for the deployment status!"
fi

rm -rf $UPSTREAM_REPO_LOCATION
