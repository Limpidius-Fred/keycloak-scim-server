package fi.metatavu.keycloak.scim.server.test.tests.functional;

import fi.metatavu.keycloak.scim.server.test.ScimClient;
import fi.metatavu.keycloak.scim.server.test.TestConsts;
import fi.metatavu.keycloak.scim.server.test.client.ApiException;
import fi.metatavu.keycloak.scim.server.test.client.model.Group;
import fi.metatavu.keycloak.scim.server.test.client.model.GroupsList;
import fi.metatavu.keycloak.scim.server.test.client.model.User;
import fi.metatavu.keycloak.scim.server.test.client.model.UsersList;
import fi.metatavu.keycloak.scim.server.test.tests.AbstractInternalAuthRealmScimTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SCIM 2.0 1-based startIndex option
 */
@Testcontainers
public class RealmOneBasedStartIndexTestsIT extends AbstractInternalAuthRealmScimTest {

    private static final String START_INDEX_BASE_ATTR = "scim.option.startIndex.base";

    @BeforeEach
    void enableOneBasedStartIndex() {
        setRealmAttribute(TestConsts.TEST_REALM, START_INDEX_BASE_ATTR, "1");
    }

    @AfterEach
    void disableOneBasedStartIndex() {
        removeRealmAttribute(TestConsts.TEST_REALM, START_INDEX_BASE_ATTR);
    }

    @Test
    void testListUsersStartIndexOneBased() throws ApiException {
        ScimClient scimClient = getAuthenticatedScimClient();

        UsersList usersList = scimClient.listUsers(null, 1, 10);

        assertEquals(1, usersList.getStartIndex());
        assertEquals(1, usersList.getTotalResults());
        assertEquals(10, usersList.getItemsPerPage());
        assertNotNull(usersList.getResources());
        assertEquals(1, usersList.getResources().size());
        assertEquals("testadmin", usersList.getResources().getFirst().getUserName());
    }

    @Test
    void testListUsersStartIndexZeroStillWorks() throws ApiException {
        ScimClient scimClient = getAuthenticatedScimClient();

        // When 1-based is enabled but startIndex=0 is passed, it should not decrement
        UsersList usersList = scimClient.listUsers(null, 0, 10);

        assertEquals(1, usersList.getStartIndex());
        assertEquals(1, usersList.getTotalResults());
        assertNotNull(usersList.getResources());
        assertEquals(1, usersList.getResources().size());
    }

    @Test
    void testListUsersPaginationOneBased() throws ApiException {
        ScimClient scimClient = getAuthenticatedScimClient();
        List<User> createdUsers = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            User user = new User();
            user.setUserName("onebased-user-" + i);
            user.setActive(true);
            user.setSchemas(List.of("urn:ietf:params:scim:schemas:core:2.0:User"));
            user.setName(getName("OneBased", "User" + i));
            user.setEmails(getEmails("onebased" + i + "@example.com"));
            createdUsers.add(scimClient.createUser(user));
        }

        try {
            // Page 1: startIndex=1 (1-based), count=2
            UsersList page1 = scimClient.listUsers("name.givenName eq \"OneBased\"", 1, 2);
            assertEquals(1, page1.getStartIndex());
            assertEquals(2, page1.getItemsPerPage());
            assertEquals(3, page1.getTotalResults());
            assertNotNull(page1.getResources());
            assertEquals(2, page1.getResources().size());

            // Page 2: startIndex=3 (1-based), count=2
            UsersList page2 = scimClient.listUsers("name.givenName eq \"OneBased\"", 3, 2);
            assertEquals(3, page2.getStartIndex());
            assertEquals(2, page2.getItemsPerPage());
            assertEquals(3, page2.getTotalResults());
            assertNotNull(page2.getResources());
            assertEquals(1, page2.getResources().size());
        } finally {
            for (User user : createdUsers) {
                deleteRealmUser(TestConsts.TEST_REALM, user.getId());
            }
        }
    }

    @Test
    void testListGroupsStartIndexOneBased() throws ApiException {
        ScimClient scimClient = getAuthenticatedScimClient();

        Group group = createGroup(scimClient, "onebased-group");

        try {
            GroupsList groupsList = scimClient.listGroups(null, 1, 10);

            assertEquals(1, groupsList.getStartIndex());
            assertNotNull(groupsList.getResources());
            assertTrue(groupsList.getResources().size() >= 1);
        } finally {
            deleteRealmGroup(TestConsts.TEST_REALM, group.getId());
        }
    }
}
