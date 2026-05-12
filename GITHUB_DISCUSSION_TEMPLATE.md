# GitHub CI/CD Discussion Template

Use this template when discussing CI/CD changes in GitHub Issues or Pull Requests.

---

## 🔧 CI/CD Workflow Change

### Description
Brief description of the CI/CD change or observation.

### Affected Workflow(s)
- [ ] Android CI/CD
- [ ] Security Scan
- [ ] Lint and Static Analysis
- [ ] Release Build

### Current Behavior
What is currently happening in the CI/CD pipeline?

### Expected Behavior
What should happen?

### Related Documentation
- [GITHUB_CI_CD.md](../GITHUB_CI_CD.md)
- [CI_CD_GUIDE.md](../CI_CD_GUIDE.md)

---

## 📋 CI/CD Enhancement Proposal

### Feature Description
New CI/CD capability or improvement.

### Benefits
Why is this enhancement needed?

### Implementation Details
High-level approach to implementation.

### Affected Files
```
.github/workflows/android-ci.yml
.github/workflows/[other files]
```

### Testing Strategy
How to verify this change?

---

## 🐛 CI/CD Issue Report

### Workflow Affected
Which workflow(s) are affected?

### Error/Issue Description
Detailed description of the problem.

### Steps to Reproduce
1. Step 1
2. Step 2
3. Step 3

### Expected Behavior
What should happen?

### Actual Behavior
What actually happens?

### Logs/Evidence
Attach or paste relevant logs:
```
[Paste logs here]
```

### Possible Cause
Any hypothesis about the root cause?

### Suggested Solution
Any ideas on how to fix this?

---

## ✅ CI/CD Health Check

### Overall Status
- Build passing: ✅ / ⚠️ / ❌
- Tests passing: ✅ / ⚠️ / ❌
- Code quality: ✅ / ⚠️ / ❌
- Security scan: ✅ / ⚠️ / ❌

### Recent Metrics
- Test Coverage: X%
- Build Time: Xs
- Number of Linting Issues: N
- Security Vulnerabilities: N

### Action Items
- [ ] Item 1
- [ ] Item 2

---

## 🎯 Release Planning

### Release Version
v1.0.0

### Release Date
YYYY-MM-DD

### Changes Included
- Feature A
- Bug Fix B

### Release Checklist
- [ ] All tests passing
- [ ] Code review approved
- [ ] Changelog updated
- [ ] Version number bumped
- [ ] Tag created: `git tag v1.0.0`
- [ ] Tag pushed: `git push origin v1.0.0`
- [ ] Release created on GitHub
- [ ] APK downloaded and verified

---

## 💬 CI/CD Discussion

### Discussion Topic
Ongoing CI/CD discussion or decision needed.

### Context
Background information.

### Question(s)
What needs to be decided?

### Options
- Option 1: [Description]
- Option 2: [Description]

### Recommendation
My recommendation and reasoning.

---

## 📊 CI/CD Metrics Discussion

### Current State
- Build Success Rate: X%
- Average Build Time: Ys
- Coverage: Z%

### Trends
What's improving/degrading?

### Goals
What metrics should we target?

### Action Plan
How to improve?

---

## 🔗 Resource Links

### Workflow Documentation
- [GITHUB_CI_CD.md](../GITHUB_CI_CD.md) - Main CI/CD reference
- [CI_CD_GUIDE.md](../CI_CD_GUIDE.md) - Developer guide

### External Resources
- [GitHub Actions Docs](https://docs.github.com/en/actions)
- [Android Developer Docs](https://developer.android.com/)

### Related Issues/PRs
- Related Issue: #123
- Related PR: #456

---

**For full CI/CD documentation, see [GITHUB_CI_CD.md](../GITHUB_CI_CD.md)**
