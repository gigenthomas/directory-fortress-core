/*
 * Copyright (c) 2009-2012. Joshua Tree Software, LLC.  All Rights Reserved.
 */

package com.jts.fortress.rest;

import com.jts.fortress.SecurityException;
import com.jts.fortress.DelegatedReviewMgr;
import com.jts.fortress.arbac.AdminRole;
import com.jts.fortress.arbac.OrgUnit;
import com.jts.fortress.arbac.UserAdminRole;
import com.jts.fortress.constants.GlobalErrIds;
import com.jts.fortress.rbac.Session;
import com.jts.fortress.rbac.User;
import com.jts.fortress.util.attr.VUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * This object implements the ARBAC02 DelegatedReviewMgr interface for performing policy interrogation of provisioned Fortress ARBAC entities
 * that reside in LDAP directory.
 * These APIs map directly to similar named APIs specified by ARBAC02 functions.  The ARBAC Functional specification describes delegated administrative
 * operations for the creation and maintenance of ARBAC element sets and relations.  Delegated administrative review functions for performing administrative queries
 * and system functions for creating and managing ARBAC attributes on user sessions and making delegated administrative access control decisions.
 * <h3>Administrative Role Based Access Control (ARBAC)</h3>
 * <img src="../../../../images/ARbac.png">
 * <p/>
 * Fortress fully supports the Oh/Sandhu/Zhang ARBAC02 model for delegated administration.  ARBAC provides large enterprises the capability to delegate administrative authority to users that reside outside of the security admin group.
 * Decentralizing administration helps because it provides security provisioning capability to work groups without sacrificing regulations for accountability or traceability.
 * <p/>

 * @author smckinn
 * @created February 13, 2012
 *
 * This object is NOT thread safe iff "adminSession" instance variable is set
 */
public class DelegatedReviewMgrRestImpl
    implements DelegatedReviewMgr
{
    static Session adminSess = null;
    private static final String OCLS_NM = DelegatedReviewMgrRestImpl.class.getName();
    private static final String USERID = "demouser4";
    private static final String PW = "password";

    /**
     * Method reads Admin Role entity from the admin role container in directory.
     *
     * @param role contains role name to be read.
     * @return AdminRole entity that corresponds with role name.
     * @throws com.jts.fortress.SecurityException
     *          will be thrown if role not found or system error occurs.
     */
    public AdminRole readRole(AdminRole role)
        throws SecurityException
    {
        VUtil.assertNotNull(role, GlobalErrIds.ARLE_NULL, OCLS_NM + ".readRole");
        AdminRole retRole;
        FortRequest request = new FortRequest();
        request.setEntity(role);
        if (this.adminSess != null)
        {
            request.setSession(adminSess);
        }
        String szRequest = RestUtils.marshal(request);
        String szResponse = RestUtils.post(USERID, PW, szRequest, Ids.Services.arleRead.toString());
        FortResponse response = RestUtils.unmarshall(szResponse);
        if (response.getErrorCode() == 0)
        {
            retRole = (AdminRole) response.getEntity();
        }
        else
        {
            throw new SecurityException(response.getErrorCode(), response.getErrorMessage());
        }
        return retRole;
    }


    /**
     * Method will return a list of type Admin Role.
     *
     * @param searchVal contains the all or some of the chars corresponding to admin role entities stored in directory.
     * @return List of type AdminRole containing role entities that match the search criteria.
     * @throws com.jts.fortress.SecurityException
     *          in the event of system error.
     */
    public List<AdminRole> findRoles(String searchVal)
        throws SecurityException
    {
        VUtil.assertNotNull(searchVal, GlobalErrIds.ARLE_NM_NULL, OCLS_NM + ".findRoles");
        List<AdminRole> retRoles;
        FortRequest request = new FortRequest();
        request.setValue(searchVal);
        if (this.adminSess != null)
        {
            request.setSession(adminSess);
        }
        String szRequest = RestUtils.marshal(request);
        String szResponse = RestUtils.post(USERID, PW, szRequest, Ids.Services.arleSearch.toString());
        FortResponse response = RestUtils.unmarshall(szResponse);
        if (response.getErrorCode() == 0)
        {
            retRoles = response.getEntities();
        }
        else
        {
            throw new SecurityException(response.getErrorCode(), response.getErrorMessage());
        }
        return retRoles;
    }

    /**
     * This function returns the set of admin roles assigned to a given user. The function is valid if and
     * only if the user is a member of the USERS data set.
     *
     * @param user contains userId matching user entity stored in the directory.
     * @return List of type UserAdminRole containing the user admin role data.
     * @throws com.jts.fortress.SecurityException If user not found or system error occurs.
     */
    public List<UserAdminRole> assignedRoles(User user)
        throws SecurityException
    {
        VUtil.assertNotNull(user, GlobalErrIds.USER_NULL, OCLS_NM + ".assignedRoles");
        List<UserAdminRole> retUserRoles = null;
        FortRequest request = new FortRequest();
        request.setEntity(user);
        if (this.adminSess != null)
        {
            request.setSession(adminSess);
        }
        String szRequest = RestUtils.marshal(request);
        String szResponse = RestUtils.post(USERID, PW, szRequest, Ids.Services.arleAsigned.toString());
        FortResponse response = RestUtils.unmarshall(szResponse);
        if (response.getErrorCode() == 0)
        {
            retUserRoles = response.getEntities();
        }
        else
        {
            throw new SecurityException(response.getErrorCode(), response.getErrorMessage());
        }
        return retUserRoles;
    }


    /**
     * This method returns the data set of all users who are assigned the given admin role.  This searches the User data set for
     * Role relationship.  This method does NOT search for hierarchical Admin Roles relationships.
     *
     * @param role contains the role name used to search the User data set.
     * @return List of type User containing the users assigned data.
     * @throws com.jts.fortress.SecurityException If system error occurs.
     */
    public List<User> assignedUsers(AdminRole role)
        throws SecurityException
    {
        VUtil.assertNotNull(role, GlobalErrIds.ARLE_NULL, OCLS_NM + ".assignedUsers");
        List<User> retUsers;
        FortRequest request = new FortRequest();
        request.setEntity(role);
        if (this.adminSess != null)
        {
            request.setSession(adminSess);
        }
        String szRequest = RestUtils.marshal(request);
        String szResponse = RestUtils.post(USERID, PW, szRequest, Ids.Services.userAsignedAdmin.toString());
        FortResponse response = RestUtils.unmarshall(szResponse);
        if (response.getErrorCode() == 0)
        {
            retUsers = response.getEntities();
            // do not return a null list to the caller:
            if(retUsers == null)
            {
                retUsers = new ArrayList<User>();
            }
        }
        else
        {
            throw new SecurityException(response.getErrorCode(), response.getErrorMessage());
        }
        return retUsers;
    }


    /**
     * Commands reads existing OrgUnit entity from OrgUnit dataset.  The OrgUnit can be either User or Perm and is
     * set by setting type attribute.
     * @param entity contains OrgUnit name and type.
     * @return
     * @throws com.jts.fortress.SecurityException in the event of data validation or system error.
     */
    public OrgUnit read(OrgUnit entity)
        throws SecurityException
    {
        VUtil.assertNotNull(entity, GlobalErrIds.ORG_NULL, OCLS_NM + ".readOrgUnit");
        OrgUnit retOrg;
        FortRequest request = new FortRequest();
        request.setEntity(entity);
        if (this.adminSess != null)
        {
            request.setSession(adminSess);
        }
        String szRequest = RestUtils.marshal(request);
        String szResponse = RestUtils.post(USERID, PW, szRequest, Ids.Services.orgRead.toString());
        FortResponse response = RestUtils.unmarshall(szResponse);
        if (response.getErrorCode() == 0)
        {
            retOrg = (OrgUnit) response.getEntity();
        }
        else
        {
            throw new SecurityException(response.getErrorCode(), response.getErrorMessage());
        }
        return retOrg;
    }


    /**
     * Commands searches existing OrgUnit entities from OrgUnit dataset.  The OrgUnit can be either User or Perm and is
     * set by setting type parameter on API.
     * @param type either PERM or USER
     * @param searchVal contains the leading chars for existing OrgUnit in OrgUnit dataset.
     * @return
     * @throws com.jts.fortress.SecurityException
     */
    public List<OrgUnit> search(OrgUnit.Type type, String searchVal)
        throws SecurityException
    {
        VUtil.assertNotNullOrEmpty(searchVal, GlobalErrIds.ORG_NULL, OCLS_NM + ".search");
        VUtil.assertNotNull(type, GlobalErrIds.ORG_TYPE_NULL, OCLS_NM + ".search");
        List<OrgUnit> retOrgs;
        FortRequest request = new FortRequest();
        OrgUnit inOrg = new OrgUnit(searchVal, type);
        request.setEntity(inOrg);
        if (this.adminSess != null)
        {
            request.setSession(adminSess);
        }
        String szRequest = RestUtils.marshal(request);
        String szResponse = RestUtils.post(USERID, PW, szRequest, Ids.Services.orgSearch.toString());
        FortResponse response = RestUtils.unmarshall(szResponse);
        if (response.getErrorCode() == 0)
        {
            retOrgs = response.getEntities();
        }
        else
        {
            throw new SecurityException(response.getErrorCode(), response.getErrorMessage());
        }
        return retOrgs;
    }


    /**
     * Setting Session into this object will enforce ARBAC controls and render this class
     * thread unsafe..
     *
     * @param session
     */
    public void setAdmin(Session session)
    {
        this.adminSess = session;
    }
}