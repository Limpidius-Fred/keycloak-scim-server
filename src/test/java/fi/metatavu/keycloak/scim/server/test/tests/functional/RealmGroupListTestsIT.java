package fi.metatavu.keycloak.scim.server.test.tests.functional;

import fi.metatavu.keycloak.scim.server.test.ScimClient;
import fi.metatavu.keycloak.scim.server.test.TestConsts;
import fi.metatavu.keycloak.scim.server.test.client.ApiException;
import fi.metatavu.keycloak.scim.server.test.client.model.*;
import fi.metatavu.keycloak.scim.server.test.tests.AbstractInternalAuthRealmScimTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SCIM 2.0 Group list endpoint
 */
@Testcontainers
public class RealmGroupListTestsIT extends AbstractInternalAuthRealmScimTest {

    @Test
    void testListGroupsIncludesMembers() throws ApiException {
        ScimClient scimClient = getAuthenticatedScimClient();

        Group group = createGroup(scimClient, "members-test-group");
        User user = createUser(scimClient, "members-test-user", "Test", "User");

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
            // Default: members should be included
            GroupsList groupsList = scimClient.listGroups(null, 0, 100);
            assertNotNull(groupsList.getResources());

            Group listedGroup = groupsList.getResources().stream()
                .filter(g -> g.getId().equals(group.getId()))
                .findFirst()
                .orElse(null);

            assertNotNull(listedGroup);
            assertNotNull(listedGroup.getMembers());
            assertEquals(1, listedGroup.getMembers().size());
            assertEquals(user.getId(), listedGroup.getMembers().getFirst().getValue());
        } finally {
            deleteRealmUser(TestConsts.TEST_REALM, user.getId());
            deleteRealmGroup(TestConsts.TEST_REALM, group.getId());
        }
    }

    @Test
    void testListGroupsWithMembersFalse() throws ApiException {
        ScimClient scimClient = getAuthenticatedScimClient();

        Group group = createGroup(scimClient, "no-members-group");
        User user = createUser(scimClient, "no-members-user", "Test", "User");

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
            // members=false: members should not be included
            GroupsList groupsList = scimClient.listGroups(null, 0, 100, false);
            assertNotNull(groupsList.getResources());

            Group listedGroup = groupsList.getResources().stream()
                .filter(g -> g.getId().equals(group.getId()))
                .findFirst()
                .orElse(null);

            assertNotNull(listedGroup);
            assertNull(listedGroup.getMembers());
            assertEquals("no-members-group", listedGroup.getDisplayName());
        } finally {
            deleteRealmUser(TestConsts.TEST_REALM, user.getId());
            deleteRealmGroup(TestConsts.TEST_REALM, group.getId());
        }
    }

    @Test
    void testListGroupsWithMembersTrue() throws ApiException {
        ScimClient scimClient = getAuthenticatedScimClient();

        Group group = createGroup(scimClient, "with-members-group");
        User user = createUser(scimClient, "with-members-user", "Test", "User");

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
            // members=true: members should be included
            GroupsList groupsList = scimClient.listGroups(null, 0, 100, true);
            assertNotNull(groupsList.getResources());

            Group listedGroup = groupsList.getResources().stream()
                .filter(g -> g.getId().equals(group.getId()))
                .findFirst()
                .orElse(null);

            assertNotNull(listedGroup);
            assertNotNull(listedGroup.getMembers());
            assertEquals(1, listedGroup.getMembers().size());
            assertEquals(user.getId(), listedGroup.getMembers().getFirst().getValue());
        } finally {
            deleteRealmUser(TestConsts.TEST_REALM, user.getId());
            deleteRealmGroup(TestConsts.TEST_REALM, group.getId());
        }
    }
}
