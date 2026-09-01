# Branch Protection Rules — Supreme

## main branch protection (apply via GitHub Settings → Branches)

### Required
- [ ] Require pull request reviews before merging (1 review minimum)
- [ ] Require status checks to pass before merging
  - Required checks: `build`, `lint`, `test`
- [ ] Require branches to be up to date before merging
- [ ] Require conversation resolution before merging
- [ ] Do not allow bypassing the above settings

### Recommended
- [ ] Require linear history (squash merges)
- [ ] Include administrators in restrictions
- [ ] Restrict who can push to matching branches (require PR)

### Apply via CLI (requires admin token):
```bash
gh api -X PUT repos/jordelmir/SupremeBass-Neon/branches/main/protection \
  -f required_status_checks='{"strict":true,"contexts":["build","lint","test"]}' \
  -f enforce_admins=true \
  -f required_pull_request_reviews='{"required_approving_review_count":1}' \
  -f restrictions=null
```
