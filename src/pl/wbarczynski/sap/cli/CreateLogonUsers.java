package pl.wbarczynski.sap.cli;

import com.sap.conn.jco.JCoContext;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoRepository;

import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;

import java.util.Properties;

public class CreateLogonUsers {
  protected static Properties getRouterProperties() {
    final Properties cp = new Properties();

    // My connection string:
    // conn=/H/saprouter.myhost/S/3297/H/I78z&amp;user=wb&amp;cInt=900

    // Our message host
    cp.setProperty(DestinationDataProvider.JCO_MSHOST, "i78z");

    // the port corresponding to our instance
    cp.setProperty(DestinationDataProvider.JCO_MSSERV, "3678");

    //
    cp.setProperty(DestinationDataProvider.JCO_GROUP, "SPACE");
    cp.setProperty(DestinationDataProvider.JCO_CLIENT, "900");
    cp.setProperty(DestinationDataProvider.JCO_USER, "wb");
    cp.setProperty(DestinationDataProvider.JCO_PASSWD, "wbsecretpass");
    cp.setProperty(DestinationDataProvider.JCO_SYSNR, "1");
    cp.setProperty(DestinationDataProvider.JCO_LANG, "en");
    cp.setProperty(DestinationDataProvider.JCO_SAPROUTER, "/H/saprouter.myhost/S/3297");
    
    return cp;
  }

  protected static Properties getDirectProperties() {
    Properties cp = new Properties();

    // /H//S/
    cp.setProperty(DestinationDataProvider.JCO_ASHOST, "/H/127.0.0.1/S/3300");
    // client number /
    cp.setProperty(DestinationDataProvider.JCO_CLIENT, "800");

    cp.setProperty(DestinationDataProvider.JCO_USER, "wb");
    cp.setProperty(DestinationDataProvider.JCO_PASSWD, "wbsecretpass");
    cp.setProperty(DestinationDataProvider.JCO_SYSNR, "1");
    cp.setProperty(DestinationDataProvider.JCO_LANG, "en");
    return cp;
  }

  public static JCoDestination getDestination() throws JCoException {
    // we do provide support for multiple destination
    // in our PropertiesDestinationDataProvider
    // so it does not matter what we give as an argument
    // to getDestination
    return JCoDestinationManager.getDestination("whatever");
  }

  protected static Properties getJcoProperties() {
    return getRouterProperties();
  }

  public static void main(String[] args) throws JCoException {

    Properties pp = getJcoProperties();
    // wrapping Properties
    PropertiesDestinationDataProvider pddp = new PropertiesDestinationDataProvider(pp);
    // registration
    Environment.registerDestinationDataProvider(pddp);

    JCoDestination dest = getDestination();

    try {
      // start the session
      JCoContext.begin(dest);

      // connect!
      JCoRepository sapRepository = dest.getRepository();
      System.out.println(sapRepository.getMonitor().getLastAccessTimestamp());
    } finally {
      // clean up
      JCoContext.end(dest);
    }
  }
}
