package com.hzf.core.base;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hzf.core.common.DataTable;
import com.hzf.core.common.SqlInfo;
import com.hzf.core.toolkit.StrUtil;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import java.util.Date;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class BaseUser extends BaseModel {

    public static final String F_GatherTOKEN = "gathertoken";
    public static final String F_GatherUser = "gatheruser";
    public static final String F_USER = "user";
    private static final byte[] SECRET = "hzfhzf7101213***777&&&".getBytes();
    private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET);

    public String GetLoginName(){
        return "";
    }
    public String GetPassword(){
        return "";
    }

    public boolean IsAdmin(){
        return false;
    }

    //重要,通过此可以设置数据权限
    public void SetQueryListDataRight(BaseQuery bq, SqlInfo suselect) throws Exception {

    }

    //重要,通过此可以设置数据权限
    public void DoSomethingQueryResult(DataTable dt, BaseQuery bq, SqlInfo su) throws Exception {

    }

    public static String FormatPwd(String pwd) {
        String p = StrUtil.toMd5("hzfhzfhzf222000111777" + pwd + "111***???");
        return p;
    }

    public static String GetTokenFromUser(BaseUser user) {
        JWTCreator.Builder jwt = JWT.create();
        jwt.withJWTId(user.id);
        jwt.withKeyId(user.GetPassword());
        jwt.withIssuer(user.getClass().getName());
        jwt.withIssuedAt(new Date());
        return jwt.sign(ALGORITHM);
    }


    public static <T extends BaseUser> T GetUserByToken(Class<T> type, String token) throws Exception {
        if (StrUtil.isEmpty(token)) return null;
        try {
            JWTVerifier verifier = JWT.require(ALGORITHM).build(); //Reusable verifier instance
            DecodedJWT jwt = verifier.verify(token);
            String id = jwt.getId();
            String password = jwt.getKeyId();
            BaseUser user = BaseUser.GetObjectById(type,id);
            if(user!=null){
                if(password.equals(user.GetPassword())){
                    return (T)user;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage(),e);
        }
        return null;
    }

}
