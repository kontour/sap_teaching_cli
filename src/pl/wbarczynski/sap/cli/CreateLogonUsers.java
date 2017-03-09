package pl.wbarczynski.sap.cli;

import com.sap.conn.jco.JCoContext;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.JCoRepository;
import com.sap.conn.jco.JCoStructure;
import com.sap.conn.jco.JCoTable;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;

import java.util.Properties;

public class CreateLogonUsers {
  protected static Properties getRouterProperties() {
    final Properties cp = new Properties();

    // My connection string:
    // conn=/H/saprouter.myhost/S/3297/H/I72z&amp;user=wb&amp;cInt=900

    // Our message host
    cp.setProperty(DestinationDataProvider.JCO_MSHOST, "i72z");

    // the port corresponding to our instance
    cp.setProperty(DestinationDataProvider.JCO_MSSERV, "3678");

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

    // conn=/H/sap_erp_host/S/3300&cInt=800&user=wb
    cp.setProperty(DestinationDataProvider.JCO_ASHOST, "/H/sap_erp_host/S/3300");
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

  public static void throwExceptionOnError(JCoFunction function) {
    JCoTable tabProfiles = function.getTableParameterList().getTable("RETURN");
    char resultType;
    for (int i = 0; i < tabProfiles.getNumRows(); i++, tabProfiles.nextRow()) {
      resultType = tabProfiles.getChar("TYPE");
      if (resultType == 'E' || resultType == 'A') {
        throw new RuntimeException(tabProfiles.getString("MESSAGE"));
      } else if (resultType == 'W') {
        System.err.println("Warning: " + tabProfiles.getString("MESSAGE"));
      }
    }
  }

  public static void viewUserFirstname(JCoDestination dest, String userName) throws JCoException {
    // http://scn.sap.com/community/scripting-languages/blog/2015/01/19/copy-abap-user-using-bapis
    JCoRepository sapRepository = dest.getRepository();
    JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_USER_GET_DETAIL");
    JCoFunction function = template.getFunction();

    function.getImportParameterList().setValue("USERNAME", userName);
    function.getImportParameterList().setValue("CACHE_RESULTS", " ");
    function.execute(dest);
    throwExceptionOnError(function);

    // The user's first name is in ADDRESS, do not ask me why.
    JCoStructure ldata = function.getExportParameterList().getStructure("ADDRESS");

    System.out.println("Type of Value" + ldata.getClassNameOfValue("FIRSTNAME"));
    System.out.println("First Name: " + ldata.getString("FIRSTNAME"));

    /*
    // To see all the fields
    JCoFieldIterator it = ldata.getFieldIterator();
    while( it.hasNextField() ) {
      JCoField f = it.nextField();
      System.out.println("name: " + f.getName() + " dt:" + f.getClassNameOfValue()
                         + " v: " + f.getValue());
    }
    */
  }

  protected static void createUser(JCoDestination dest, String userName,
      String password,
      String[] profiles) throws JCoException {

    JCoRepository sapRepository = dest.getRepository();
    JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_USER_CREATE1");
    JCoFunction function = template.getFunction();

    // password is a structure
    JCoStructure strPassword = function.getImportParameterList().getStructure("PASSWORD");
    strPassword.setValue("BAPIPWD", password);

    function.getImportParameterList().setValue("USERNAME", userName);
    function.getImportParameterList().setValue("PASSWORD", strPassword);
    function.execute(dest);
    throwExceptionOnError(function);
    assingPofilesToUser(dest, userName, profiles);
  }

  private static void assingPofilesToUser(JCoDestination dest,
      String userName, String[] profiles) throws JCoException {
    JCoRepository sapRepository = dest.getRepository();
    JCoFunctionTemplate template = sapRepository.getFunctionTemplate("BAPI_USER_PROFILES_ASSIGN");
    JCoFunction function = template.getFunction();

    JCoTable tabProfiles = function.getTableParameterList().getTable("PROFILES");
    for (String p : profiles) {
      tabProfiles.appendRow();
      tabProfiles.setValue("BAPIPROF", p);
    }
    function.getImportParameterList().setValue("USERNAME", userName);
    function.getTableParameterList().setValue("PROFILES", tabProfiles);
    function.execute(dest);
    throwExceptionOnError(function);
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

      viewUserFirstname(dest, "my_user_name");
      createUser(dest, "johndoe", "johndoe_secret", new String[] {"IDES_USER"});
      createUser(dest, "powerdoe", "powerdoe_secret", new String[] {"SAP_ALL", "IDES_USER"});

      // connect!
      /*JCoRepository sapRepository = dest.getRepository();
      System.out.println( new Date(sapRepository.getMonitor().getLastAccessTimestamp()));*/
    } finally {
      // clean up
      JCoContext.end(dest);
    }
  }
}
