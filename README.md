# GitHubExplorer
Explore repositories and download files

This is a simple app that is a slight refinement of what I did for a coding test. It allows you to see a user's repository (with optional login and 2FA) and navigate through and access the file structure.

It connects to the GitHub API to authenticate, and if it receives a 401 response it requests a 2FA code from the user (GitHub should automatically send out an SMS if SMS 2FA is active). Then it simply passes that authentication token to the next activity which lets the user select a repo and browse through/access its files.

For a proper app to explore GitHub, try https://github.com/k0shk0sh/FastHub
