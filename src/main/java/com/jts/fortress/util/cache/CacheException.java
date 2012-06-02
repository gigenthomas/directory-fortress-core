/*
 * Copyright (c) 2009-2012. Joshua Tree Software, LLC.  All Rights Reserved.
 */
package com.jts.fortress.util.cache;

import com.jts.fortress.BaseRuntimeException;


/**
 * This exception extends {@code BaseRuntimeException} and is thrown when Fortress caching operation failed.
 * This is critical runtime exception and means system is inoperable due to a caching error.
 * See the {@link com.jts.fortress.constants.GlobalErrIds} javadoc for list of error ids.
 *
 * @author Shawn McKinney
 * @created March 9, 2012
 */
public class CacheException extends BaseRuntimeException
{
    private int subsystem;
    private Exception m_Exception;
    private Object moreInfo;

    /**
     * Create exception with error id, message and related exception.
     * @param errorId contains error code that is contained within {@link com.jts.fortress.constants.GlobalErrIds}
     * @param newMsgText contains text related to the exception.
     * @param newException contains related exception.
     */
    public CacheException(int errorId, String newMsgText, Exception newException)
    {
        super(errorId, newMsgText, newException);
        this.m_Exception = newException;
    }

    /**
     * Create exception with error id and message.
     * @param errorId contains error code that is contained within {@link com.jts.fortress.constants.GlobalErrIds}
     * @param newMsgText contains text related to the exception.
     */
    public CacheException(int errorId, String newMsgText)
    {
        super(errorId, newMsgText);
    }
}
