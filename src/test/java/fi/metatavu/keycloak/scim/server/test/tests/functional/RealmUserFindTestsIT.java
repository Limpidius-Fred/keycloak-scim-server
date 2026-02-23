package fi.metatavu.keycloak.scim.server.test.tests.functional;

import fi.metatavu.keycloak.scim.server.test.tests.AbstractInternalAuthRealmScimTest;
import fi.metatavu.keycloak.scim.server.test.ScimClient;
import fi.metatavu.keycloak.scim.server.test.TestConsts;
import fi.metatavu.keycloak.scim.server.test.client.ApiException;
import fi.metatavu.keycloak.scim.server.test.client.model.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SCIM 2.0 user find (GET /Users/{id}) endpoint
 */
@Testcontainers
public class RealmUserFindTestsIT extends AbstractInternalAuthRealmScimTest {

    @Test
    void testFindUserById() throws ApiException {
        ScimClient scimClient = getAuthenticatedScimClient();

        // Create user
        User user = new User();
        user.setUserName("find-me");
        user.setActive(true);
        user.setSchemas(List.of("urn:ietf:params:scim:schemas:core:2.0:User"));
        user.setName(getName("Find", "Me"));
        user.setEmails(getEmails("find.me@example.com"));

        User created = scimClient.createUser(user);
        assertNotNull(created);
        String userId = created.getId();

        // Find the user
        User found = scimClient.findUser(userId);
        assertNotNull(found);
        assertEquals(userId, found.getId());
        assertEquals("find-me", found.getUserName());
        assertNotNull(found.getName());
        assertEquals("Find", found.getName().getGivenName());
        assertEquals("Me", found.getName().getFamilyName());
        assertNotNull(found.getEmails());
        assertEquals("find.me@example.com", found.getEmails().getFirst().getValue());

        // Clean up
        deleteRealmUser(TestConsts.TEST_REALM, userId);
    }

    @Test
    void testFindUserIncludesGroups() throws ApiException {
        ScimClient scimClient = getAuthenticatedScimClient();

        User user = createUser(scimClient, "groups-user", "Groups", "User");
        Group group = createGroup(scimClient, "user-group");

        // Add user to group
        PatchRequest patchRequest = new PatchRequest();
        patchRequest.setSchemas(List.of("urn:ietf:params:scim:api:messages:2.0:PatchOp"));
        PatchRequestOperationsInner operation = new PatchRequestOperationsInner();
        operation.setOp("add");
        operation.setPath("members");
        GroupMembersInner member = new GroupMembersInner();
        member.setValue(user.getId());
        operation.setValue(Collections.singletonList(member));
        patchRequest.setOperations(List.of(operation));
        scimClient.patchGroup(group.getId(), patchRequest);

        try {
            User found = scimClient.findUser(user.getId());
            assertNotNull(found);

            Object groupsObj = found.getAdditionalProperty("groups");
            assertNotNull(groupsObj, "groups attribute should be present");

            @SuppressWarnings("unchecked")
            List<Map<String, String>> groups = (List<Map<String, String>>) groupsObj;
            assertEquals(1, groups.size());

            Map<String, String> groupEntry = groups.getFirst();
            assertEquals(group.getId(), groupEntry.get("value"));
            assertEquals("user-group", groupEntry.get("display"));
            assertNotNull(groupEntry.get("$ref"));
            assertTrue(groupEntry.get("$ref").contains("Groups/" + group.getId()));
        } finally {
            deleteRealmUser(TestConsts.TEST_REALM, user.getId());
            deleteRealmGroup(TestConsts.TEST_REALM, group.getId());
        }
    }

    @Test
    void testFindUserWithNoGroupsOmitsGroupsAttribute() throws ApiException {
        ScimClient scimClient = getAuthenticatedScimClient();

        User user = createUser(scimClient, "no-groups-user", "NoGroups", "User");

        try {
            User found = scimClient.findUser(user.getId());
            assertNotNull(found);
            assertNull(found.getAdditionalProperty("groups"), "groups attribute should not be present when user has no groups");
        } finally {
            deleteRealmUser(TestConsts.TEST_REALM, user.getId());
        }
    }

    @Test
    void testListUsersDoesNotIncludeGroups() throws ApiException {
        ScimClient scimClient = getAuthenticatedScimClient();

        User user = createUser(scimClient, "list-groups-user", "List", "User");
        Group group = createGroup(scimClient, "list-user-group");

        // Add user to group
        PatchRequest patchRequest = new PatchRequest();
        patchRequest.setSchemas(List.of("urn:ietf:params:scim:api:messages:2.0:PatchOp"));
        PatchRequestOperationsInner operation = new PatchRequestOperationsInner();
        operation.setOp("add");
        operation.setPath("members");
        GroupMembersInner member = new GroupMembersInner();
        member.setValue(user.getId());
        operation.setValue(Collections.singletonList(member));
        patchRequest.setOperations(List.of(operation));
        scimClient.patchGroup(group.getId(), patchRequest);

        try {
            UsersList usersList = scimClient.listUsers("userName eq \"list-groups-user\"", 0, 10);
            assertNotNull(usersList.getResources());
            assertEquals(1, usersList.getResources().size());

            User listedUser = usersList.getResources().getFirst();
            assertNull(listedUser.getAdditionalProperty("groups"), "groups attribute should not be present in list response");
        } finally {
            deleteRealmUser(TestConsts.TEST_REALM, user.getId());
            deleteRealmGroup(TestConsts.TEST_REALM, group.getId());
        }
    }

    @Test
    void testCreateUserDoesNotIncludeGroups() throws ApiException {
        ScimClient scimClient = getAuthenticatedScimClient();

        User user = new User();
        user.setUserName("create-groups-user");
        user.setActive(true);
        user.setSchemas(List.of("urn:ietf:params:scim:schemas:core:2.0:User"));
        user.setName(getName("Create", "User"));
        user.setEmails(getEmails("create.groups@example.com"));

        User created = scimClient.createUser(user);

        try {
            assertNotNull(created);
            assertNull(created.getAdditionalProperty("groups"), "groups attribute should not be present in create response");
        } finally {
            deleteRealmUser(TestConsts.TEST_REALM, created.getId());
        }
    }

    @Test
    void testFindUserNotFound() {
        ScimClient scimClient = getAuthenticatedScimClient();

        String fakeId = "non-existent-id";

        ApiException exception = assertThrows(ApiException.class, () ->
                scimClient.findUser(fakeId)
        );

        assertEquals(404, exception.getCode());
    }

}