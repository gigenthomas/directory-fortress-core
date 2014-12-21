/*

 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.fortress.core.ldap.group;


import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.fortress.core.FinderException;
import org.apache.directory.fortress.core.ObjectFactory;
import org.apache.directory.fortress.core.UpdateException;
import org.apache.directory.fortress.core.cfg.Config;
import org.apache.directory.fortress.core.ldap.ApacheDsDataProvider;
import org.apache.directory.fortress.core.rbac.User;
import org.apache.directory.fortress.core.util.attr.AttrHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.directory.fortress.core.CreateException;
import org.apache.directory.fortress.core.GlobalErrIds;
import org.apache.directory.fortress.core.GlobalIds;
import org.apache.directory.fortress.core.RemoveException;
import org.apache.directory.fortress.core.util.attr.VUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the Group node for LDAP Directory Information Tree.
 * This class is thread safe.
 *
 * @author Shawn McKinney
 */
final class GroupDAO extends ApacheDsDataProvider
{
    private static final String CLS_NM = GroupDAO.class.getName();
    private static final Logger LOG = LoggerFactory.getLogger( CLS_NM );
    private static final String GROUP_OBJECT_CLASS = "group.objectclass";
    private static final String GROUP_OBJECT_CLASS_IMPL = Config.getProperty( GROUP_OBJECT_CLASS );
    private static final String GROUP_PROTOCOL_ATTR = "group.protocol";
    private static final String GROUP_PROTOCOL_ATTR_IMPL = Config.getProperty( GROUP_PROTOCOL_ATTR );
    private static final String GROUP_PROPERTY_ATTR = "group.properties";
    private static final String GROUP_PROPERTY_ATTR_IMPL = Config.getProperty( GROUP_PROPERTY_ATTR );
    private static final String GROUP_OBJ_CLASS[] = {SchemaConstants.TOP_OC, GROUP_OBJECT_CLASS_IMPL};
    private static final String[] GROUP_ATRS = {SchemaConstants.CN_AT, SchemaConstants.DESCRIPTION_AT, GROUP_PROTOCOL_ATTR_IMPL, GROUP_PROPERTY_ATTR_IMPL, SchemaConstants.MEMBER_AT};

    /**
     * Package private default constructor.
     */
    GroupDAO()
    {
    }

    /**
     * @param group
     * @throws org.apache.directory.fortress.core.CreateException
     *
     */
    final Group create( Group group ) throws CreateException
    {
        LdapConnection ld = null;
        String nodeDn = getDn( group.getName(), group.getContextId() );
        
        try
        {
            LOG.debug( "create group dn [{}]", nodeDn );
            Entry myEntry = new DefaultEntry( nodeDn );
            myEntry.add( SchemaConstants.OBJECT_CLASS_AT, GROUP_OBJ_CLASS );
            myEntry.add( SchemaConstants.CN_AT , group.getName() );
            myEntry.add( GROUP_PROTOCOL_ATTR_IMPL, group.getProtocol() );
            loadAttrs( group.getMembers(), myEntry, SchemaConstants.MEMBER_AT );
            loadProperties( group.getProperties(), myEntry, GROUP_PROPERTY_ATTR_IMPL, '=' );
            
            if ( VUtil.isNotNullOrEmpty( group.getDescription() ) )
            {
                myEntry.add( SchemaConstants.DESCRIPTION_AT, group.getDescription() );
            }
            
            ld = getAdminConnection();
            add( ld, myEntry );
        }
        catch ( LdapException e )
        {
            String error = "create group node dn [" + nodeDn + "] caught LDAPException=" + e.getMessage();
            throw new CreateException( GlobalErrIds.GROUP_ADD_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }
        
        return group;
    }


    /**
     * @param group
     * @return
     * @throws org.apache.directory.fortress.core.CreateException
     *
     */
    final Group update( Group group ) throws FinderException, UpdateException
    {
        LdapConnection ld = null;
        String nodeDn = getDn( group.getName(), group.getContextId() );
        
        try
        {
            LOG.debug( "update group dn [{}]", nodeDn );
            List<Modification> mods = new ArrayList<Modification>();
            
            if ( VUtil.isNotNullOrEmpty( group.getDescription() ) )
            {
                mods.add( new DefaultModification(
                    ModificationOperation.REPLACE_ATTRIBUTE, SchemaConstants.DESCRIPTION_AT, group.getDescription() ) );
            }
            
            if ( VUtil.isNotNullOrEmpty( group.getProtocol() ) )
            {
                mods.add( new DefaultModification(
                    ModificationOperation.REPLACE_ATTRIBUTE, GROUP_PROTOCOL_ATTR_IMPL, group.getProtocol() ) );
            }
            
            loadAttrs( group.getMembers(), mods, SchemaConstants.MEMBER_AT );
            loadProperties( group.getProperties(), mods, GROUP_PROPERTY_ATTR_IMPL, true, '=' );
            
            if ( mods.size() > 0 )
            {
                ld = getAdminConnection();
                modify( ld, nodeDn, mods, group );
            }
        }
        catch ( LdapException e )
        {
            String error = "update group node dn [" + nodeDn + "] caught LDAPException=" + e.getMessage();
            throw new UpdateException( GlobalErrIds.GROUP_UPDATE_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }
        return get( group );
    }

    final Group add( Group group, String key, String value ) throws FinderException, CreateException
    {
        LdapConnection ld = null;
        String nodeDn = getDn( group.getName(), group.getContextId() );
        
        try
        {
            LOG.debug( "add group property dn [{}], key [{}], value [{}]", nodeDn, key, value );
            List<Modification> mods = new ArrayList<Modification>();
            mods.add( new DefaultModification(
                ModificationOperation.ADD_ATTRIBUTE, GROUP_PROPERTY_ATTR_IMPL, key + "=" + value ) );
            ld = getAdminConnection();
            modify( ld, nodeDn, mods, group );
        }
        catch ( LdapException e )
        {
            String error = "update group property node dn [" + nodeDn + "] caught LDAPException=" + e.getMessage();
            throw new CreateException( GlobalErrIds.GROUP_ADD_PROPERTY_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }
        
        return get( group );
    }


    final Group delete( Group group, String key, String value ) throws FinderException, RemoveException
    {
        LdapConnection ld = null;
        String nodeDn = getDn( group.getName(), group.getContextId() );
        
        try
        {
            LOG.debug( "delete group property dn [{}], key [{}], value [{}]", nodeDn, key, value );
            List<Modification> mods = new ArrayList<Modification>();
            mods.add( new DefaultModification(
                ModificationOperation.REMOVE_ATTRIBUTE, GROUP_PROPERTY_ATTR_IMPL, key + "=" + value ) );
            ld = getAdminConnection();
            modify( ld, nodeDn, mods, group );
        }
        catch ( LdapException e )
        {
            String error = "delete group property node dn [" + nodeDn + "] caught LDAPException=" + e.getMessage();
            throw new RemoveException( GlobalErrIds.GROUP_DELETE_PROPERTY_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }
        return get( group );
    }

    /**
     * This method will remove group node from diretory.
     *
     * @param group
     * @throws org.apache.directory.fortress.core.RemoveException
     *
     */
    final Group remove( Group group ) throws RemoveException
    {
        LdapConnection ld = null;
        String nodeDn = getDn( group.getName(), group.getContextId() );
        LOG.debug( "remove group dn [{}]", nodeDn );
        try
        {
            ld = getAdminConnection();
            deleteRecursive( ld, nodeDn );
        }
        catch ( CursorException e )
        {
            String error = "remove group node dn [" + nodeDn + "] caught CursorException="
                + e.getMessage();
            throw new RemoveException( GlobalErrIds.GROUP_DELETE_FAILED, error, e );
        }
        catch ( LdapException e )
        {
            String error = "remove group node dn [" + nodeDn + "] caught LDAPException=" + e.getMessage();
            throw new RemoveException( GlobalErrIds.GROUP_DELETE_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }
        return group;
    }

    /**
     * @param entity
     * @param userDn
     * @return
     * @throws org.apache.directory.fortress.core.UpdateException
     *
     */
    final Group assign( Group entity, String userDn ) throws FinderException, UpdateException
    {
        LdapConnection ld = null;
        String dn = getDn( entity.getName(), entity.getContextId() );
        LOG.debug( "assign group property dn [{}], member dn [{}]", dn, userDn );
        try
        {
            List<Modification> mods = new ArrayList<Modification>();
            mods.add( new DefaultModification(
                ModificationOperation.ADD_ATTRIBUTE, SchemaConstants.MEMBER_AT, userDn ) );
            ld = getAdminConnection();
            modify( ld, dn, mods, entity );
        }
        catch ( LdapException e )
        {
            String error = "assign group name [" + entity.getName() + "] user dn [" + userDn + "] caught " +
                "LDAPException=" + e.getMessage();
            throw new UpdateException( GlobalErrIds.GROUP_USER_ASSIGN_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }
        return get( entity );
    }

    /**
     * @param entity
     * @param userDn
     * @return
     * @throws org.apache.directory.fortress.core.UpdateException
     *
     */
    final Group deassign( Group entity, String userDn ) throws FinderException, UpdateException
    {
        LdapConnection ld = null;
        String dn = getDn( entity.getName(), entity.getContextId() );
        LOG.debug( "deassign group property dn [{}], member dn [{}]", dn, userDn );
        
        try
        {
            List<Modification> mods = new ArrayList<Modification>();
            mods.add( new DefaultModification(
                ModificationOperation.REMOVE_ATTRIBUTE, SchemaConstants.MEMBER_AT, userDn ) );

            ld = getAdminConnection();
            modify( ld, dn, mods, entity );
        }
        catch ( LdapException e )
        {
            String error = "deassign group name [" + entity.getName() + "] user dn [" + userDn + "] caught " +
                "LDAPException=" + e.getMessage();
            throw new UpdateException( GlobalErrIds.GROUP_USER_DEASSIGN_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }
        
        return get( entity );
    }

    /**
     * @param group
     * @return
     * @throws org.apache.directory.fortress.core.FinderException
     *
     */
    final Group get( Group group ) throws FinderException
    {
        Group entity = null;
        LdapConnection ld = null;
        String dn = getDn( group.getName(), group.getContextId() );
        
        try
        {
            ld = getAdminConnection();
            Entry findEntry = read( ld, dn, GROUP_ATRS );
            entity = unloadLdapEntry( findEntry, 0 );
            
            if ( entity == null )
            {
                String warning = "read no entry found dn [" + dn + "]";
                throw new FinderException( GlobalErrIds.GROUP_NOT_FOUND, warning );
            }
        }
        catch ( LdapNoSuchObjectException e )
        {
            String warning = "read Obj COULD NOT FIND ENTRY for dn [" + dn + "]";
            throw new FinderException( GlobalErrIds.GROUP_NOT_FOUND, warning );
        }
        catch ( LdapException e )
        {
            String error = "read dn [" + dn + "] LdapException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.GROUP_READ_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }
        return entity;
    }

    /**
     * @param group
     * @return
     * @throws org.apache.directory.fortress.core.FinderException
     *
     */
    final List<Group> find( Group group ) throws FinderException
    {
        List<Group> groupList = new ArrayList<>();
        LdapConnection ld = null;
        SearchCursor searchResults;
        String groupRoot = getRootDn( group.getContextId(), GlobalIds.GROUP_ROOT );
        String filter = null;
        
        try
        {
            String searchVal = encodeSafeText( group.getName(), GlobalIds.ROLE_LEN );
            filter = GlobalIds.FILTER_PREFIX + GROUP_OBJECT_CLASS_IMPL + ")(" + SchemaConstants.CN_AT + "=" + searchVal + "*))";
            ld = getAdminConnection();
            searchResults = search( ld, groupRoot, SearchScope.ONELEVEL, filter, GROUP_ATRS, false,
                GlobalIds.BATCH_SIZE );
            long sequence = 0;
            while ( searchResults.next() )
            {
                groupList.add( unloadLdapEntry( searchResults.getEntry(), sequence++ ) );
            }
        }
        catch ( CursorException e )
        {
            String error = "find filter [" + filter + "] caught CursorException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.GROUP_SEARCH_FAILED, error, e );
        }
        catch ( LdapException e )
        {
            String error = "find filter [" + filter + "] caught LDAPException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.GROUP_SEARCH_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }
        
        return groupList;
    }

    /**
     * @param user
     * @return
     * @throws org.apache.directory.fortress.core.FinderException
     *
     */
    final List<Group> find( User user ) throws FinderException
    {
        List<Group> groupList = new ArrayList<>();
        LdapConnection ld = null;
        SearchCursor searchResults;
        String groupRoot = getRootDn( user.getContextId(), GlobalIds.GROUP_ROOT );
        String filter = null;
        
        try
        {
            encodeSafeText( user.getUserId(), GlobalIds.USERID_LEN );
            filter = GlobalIds.FILTER_PREFIX + GROUP_OBJECT_CLASS_IMPL + ")(" + SchemaConstants.MEMBER_AT + "=" + user.getDn() + "))";
            ld = getAdminConnection();
            searchResults = search( ld, groupRoot, SearchScope.ONELEVEL, filter, GROUP_ATRS, false,
                GlobalIds.BATCH_SIZE );
            long sequence = 0;
            
            while ( searchResults.next() )
            {
                groupList.add( unloadLdapEntry( searchResults.getEntry(), sequence++ ) );
            }
        }
        catch ( CursorException e )
        {
            String error = "find filter [" + filter + "] caught CursorException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.GROUP_SEARCH_FAILED, error, e );
        }
        catch ( LdapException e )
        {
            String error = "find filter [" + filter + "] caught LDAPException=" + e.getMessage();
            throw new FinderException( GlobalErrIds.GROUP_SEARCH_FAILED, error, e );
        }
        finally
        {
            closeAdminConnection( ld );
        }
        
        return groupList;
    }
    

    /**
     * @param le
     * @param sequence
     * @return
     * @throws LdapException
     */
    private Group unloadLdapEntry( Entry le, long sequence )
        throws LdapInvalidAttributeValueException
    {
        Group entity = new ObjectFactory().createGroup();
        entity.setName( getAttribute( le, SchemaConstants.CN_AT ) );
        entity.setDescription( getAttribute( le, SchemaConstants.DESCRIPTION_AT ) );
        entity.setProtocol( getAttribute( le, GROUP_PROTOCOL_ATTR_IMPL ) );
        entity.setMembers( getAttributes( le, SchemaConstants.MEMBER_AT ) );
        entity.setMemberDn( true );
        entity.setProperties( AttrHelper.getProperties( getAttributes( le, GROUP_PROPERTY_ATTR_IMPL ), '=' ) );
        entity.setSequenceId( sequence );
        
        return entity;
    }
    

    private String getDn( String name, String contextId )
    {
        return SchemaConstants.CN_AT + "=" + name + "," + getRootDn( contextId, GlobalIds.GROUP_ROOT );
    }
}