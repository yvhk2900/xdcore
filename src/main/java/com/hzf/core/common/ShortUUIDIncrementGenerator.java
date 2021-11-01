package com.hzf.core.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Properties;
import java.util.UUID;

public class ShortUUIDIncrementGenerator implements IdentifierGenerator, Configurable {
    private static final Log log = LogFactory.getLog(ShortUUIDIncrementGenerator.class);

    @Override
    public void configure(Type arg0, Properties arg1, ServiceRegistry arg2)
            throws MappingException {

    }

    @Override
    public Serializable generate(SharedSessionContractImplementor sharedSessionContractImplementor, Object arg1) throws HibernateException {
        String result = getUUID();
        return result;
    }
//    public static long GuidToInt64()
//    {
//        byte[] bytes = Guid.NewGuid().ToByteArray();
//        return BitConverter.ToInt64(bytes, 0);
//    }
    public synchronized static String getUUID() {
        return UUID.randomUUID().toString().toUpperCase().replaceAll("-", "");
    }

}
