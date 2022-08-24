package edu.mcw.rgd;

import edu.mcw.rgd.dao.AbstractDAO;

/**
 * @author mtutaj
 * @since 08/24/2022
 * wrapper to handle all DAO code
 */
public class Dao {

    AbstractDAO dao = new AbstractDAO();

    public String getConnectionInfo() {
        return dao.getConnectionInfo();
    }

}
