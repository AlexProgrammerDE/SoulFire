# Microsoft Auth

To authenticate with Minecraft servers using Microsoft accounts, you must first create a Microsoft application in the [Azure Portal](https://portal.azure.com/). You can follow the steps below to create a new application.
The resulting client id has to be added to the microsoft client id field in the account page if you wish to use microsoft auth.

The following steps will give you an MSA client ID. It is confidential and should not be reused.
You can obtain one for yourself by using azure app registration:
https://docs.microsoft.com/en-us/azure/active-directory/develop/quickstart-register-app

The app registration should:
- Be only for personal accounts.
- Not have any redirect URI.
- Not have any platform.
- Have no credentials.
- No certificates.
- No client secrets.
- Enable 'Live SDK support' for access to XBox APIs. 
- Enable 'public client flows' for OAuth2 device flow. (Authentication)

By putting one in here, you accept the terms and conditions for using the MS Identity Platform and assume all responsibilities associated with it.
See: https://docs.microsoft.com/en-us/legal/microsoft-identity-platform/terms-of-use
