import groovy.json.JsonSlurper

// Script must be called from the script directory to pick up the imported Class.
import SonarApiClient

static List<Tuple> buildGroupsData() {
    def jsonSlurper = new JsonSlurper()
    def reader = new BufferedReader(new InputStreamReader(new FileInputStream("groups.json"), "UTF-8"))
    def data = jsonSlurper.parse(reader)

    List<Tuple> groups = []

    data.groups.each {
        Tuple groupsTuple = new Tuple(it.name, it.description, it.globalPermissions, it.projectPermissions)
        System.out.println("Adding group ${groupsTuple}")
        groups << groupsTuple
    }

    return groups
}

static void addPermission(groupName, permission, permissionsUrl) {
    System.out.println("Adding permission ${permission} for ${groupName}")

    def groupNamePair = SonarApiClient.buildSingleValuedKeyPair('groupName', groupName)
    def permissionsPair = SonarApiClient.buildSingleValuedKeyPair('permission', permission)
    def queryValues = [groupNamePair, permissionsPair]
    def queryString = SonarApiClient.buildQueryString(queryValues.iterator())
    def querySucceeded = SonarApiClient.postQueryString(permissionsUrl, queryString)

    if (!querySucceeded) {
        System.exit(1)
    }
}

def sonarApiGroupsUrl = SonarApiClient.sonarApiUrl + 'user_groups/create'
def sonarApiGlobalPermissionsUrl = SonarApiClient.sonarApiUrl + 'permissions/add_group'
def sonarApiProjectPermissionsUrl = SonarApiClient.sonarApiUrl + 'permissions/add_group_to_template'

for (group in buildGroupsData()) {
    System.out.println("Adding group ${group}")

    def name = SonarApiClient.buildSingleValuedKeyPair('name', group.get(0))
    def description = SonarApiClient.buildSingleValuedKeyPair('description', group.get(1))
    def queryValues = [name, description]
    def queryString = SonarApiClient.buildQueryString(queryValues.iterator())
    def querySucceeded = SonarApiClient.postQueryString(sonarApiGroupsUrl, queryString)

    if (!querySucceeded) {
        System.exit(1)
    }

    def globalPermissionsList = group.get(2)

    if (globalPermissionsList && !globalPermissionsList.allWhitespace) {
        System.out.println("Adding global permissions list ${globalPermissionsList}")

        def globalPermissions = globalPermissionsList.tokenize(',')

        for (globalPermission in globalPermissions) {
            addPermission(group.get(0), globalPermission, sonarApiGlobalPermissionsUrl)
        }
    }

    def projectPermissionsList = group.get(3)

    if (projectPermissionsList && !projectPermissionsList.allWhitespace) {
        System.out.println("Adding project permissions list ${projectPermissionsList}")

        def projectPermissions = projectPermissionsList.tokenize(',')

        for (projectPermission in projectPermissions) {
            addPermission(group.get(0), projectPermission, sonarApiProjectPermissionsUrl)
        }
    }
}

System.exit(0)