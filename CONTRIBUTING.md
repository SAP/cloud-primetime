# Contributing to PrimeTime

You want to contribute to PrimeTime? Welcome! Please read this document to understand what you can do:
 * [Analyze Issues](#analyze-issues)
 * [Report an Issue](#report-an-issue)
 * [Contribute Code](#contribute-code)

## Analyze Issues

Analyzing issue reports can be a lot of effort. Any help is welcome!
Go to [the Github issue tracker](https://github.com/SAP/cloud-primetime/issues?state=open) and find an open issue which needs additional work or a bugfix. Maybe you can even find and [contribute](#contribute-code) a bugfix?

## Report an Issue

You can go to the [Github issue tracker for PrimeTime](https://github.com/SAP/cloud-primetime/issues/new) to report the issue.

## Contribute Code

You are welcome to contribute code to PrimeTime in order to fix bugs or to implement new features.

There are three important things to know:

1.  You must be aware of the Apache License (which describes contributions) and **agree to the Contributors License Agreement**. This is common practice in all major Open Source projects. To make this process as simple as possible, we are using *[CLA assistant](https://cla-assistant.io/)* for individual contributions. CLA assistant is an open source tool that integrates with GitHub very well and enables a one-click-experience for accepting the CLA. For company contributers special rules apply. See the respective section below for details.
2.  There are **several requirements regarding code style, quality, and product standards** which need to be met (we also have to follow them). The respective section below gives more details on the coding guidelines.
3.  **Not all proposed contributions can be accepted**. Some features may e.g. just fit a third-party add-on better. The code must fit the overall direction of PrimeTime and really improve it, so there should be some "bang for the byte". For most bug fixes this is a given, but major feature implementation first need to be discussed with one of the PrimeTime committers, possibly one who touched the related code recently. The more effort you invest, the better you should clarify in advance whether the contribution fits: the best way would be to just open an enhancement ticket in the issue tracker to discuss the feature you plan to implement (make it clear you intend to contribute). We will then forward the proposal to the respective code owner, this avoids disappointment.

### Contributor License Agreement

When you contribute (code, documentation, or anything else), you have to be aware that your contribution is covered by the same [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0) that is applied to PrimeTime itself. In particular you need to agree to the Individual Contributor License Agreement, which can be [found here](https://gist.github.com/CLAassistant/bd1ea8ec8aa0357414e8).
(This applies to all contributors, including those contributing on behalf of a company). If you agree to its content, you simply have to click on the link posted by the CLA assistant as a comment to the pull request. Click it to check the CLA, then accept it on the following screen if you agree to it. CLA assistant will save this decision for upcoming contributions and will notify you if there is any change to the CLA in the meantime.

#### Company Contributors

If employees of a company contribute code, in **addition** to the individual agreement above, there needs to be one company agreement submitted. This is mainly for the protection of the contributing employees.

A company representative authorized to do so needs to download, fill, and print
the [Corporate Contributor License Agreement](/docs/SAP%20Corporate%20Contributor%20License%20Agreement.pdf) form. Then either:

-   Scan it and e-mail it to [opensource@sap.com](mailto:opensource@sap.com) and [SAP_PrimeTime@sap.com](mailto:SAP_PrimeTime@sap.com)
-   Fax it to: +49 6227 78-45813
-   Send it by traditional letter to: *Industry Standards & Open Source Team, Dietmar-Hopp-Allee 16, 69190 Walldorf, Germany*

The form contains a list of employees who are authorized to contribute on behalf of your company. When this list changes, please let us know.

### Contribution Content Guidelines

Contributed content can be accepted if it:

1. is useful to improve PrimeTime (explained above)
2. follows the applicable guidelines and standards

### How to contribute - the Process

1.  Make sure the change would be welcome (e.g. a bugfix or a useful feature); best do so by proposing it in a GitHub issue
2.  Create a branch forking the primetime repository and do your change
3.  Commit and push your changes on that branch
    -   When you have several commits, squash them into one (see [this explanation](http://davidwalsh.name/squash-commits-git)) - this also needs to be done when additional changes are required after the code review

4.  In the commit message follow the [commit message guidelines](docs/guidelines.md#git-guidelines)
5.  If your change fixes an issue reported at GitHub, add the following line to the commit message:
    - ```Fixes https://github.com/SAP/cloud-primetime/issues/(issueNumber)```
    - Do NOT add a colon after "Fixes" - this prevents automatic closing.
	- When your pull request number is known (e.g. because you enhance a pull request after a code review), you can also add the line ```Closes https://github.com/SAP/cloud-primetime/pull/(pullRequestNumber)```
6.  Create a Pull Request to github.com/SAP/cloud-primetime
7.  Follow the link posted by the CLA assistant to your pull request and accept it, as described in detail above.
8.  Wait for our code review and approval, possibly enhancing your change on request
    -   Note that the PrimeTime developers also have their regular duties, so depending on the required effort for reviewing, testing and clarification this may take a while

9.  Once the change has been approved we will inform you in a comment
10.  Your pull request cannot be merged directly into the branch (internal SAP processes), but will be merged internally and immediately appear in the public repository as well. Pull requests for non-code branches (like "gh-pages" for the website) can be directly merged.
11.  We will close the pull request, feel free to delete the now obsolete branch