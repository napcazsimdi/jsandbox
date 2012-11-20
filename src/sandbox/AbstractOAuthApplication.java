package sandbox;

import java.awt.Desktop;
import java.io.Console;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;


import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

/**
 *
 *  AbstractOAuthApplication
 *
 */
public abstract class AbstractOAuthApplication
	{
	protected static Logger LOG=Logger.getLogger("oauth");
	private OAuthService service;
	private org.scribe.model.Token accessToken;
	private Preferences preferences;
	private boolean force_manual_access=false;
	private boolean force_ignore_prefs=false;
	
	protected String api_key=null;
	protected String api_secret=null;

	
	protected AbstractOAuthApplication()
		{
		}
	
	protected abstract String getPreferencesPrefix();
		
	
	protected OAuthService getService()
		{
		return service;
		}
	
	protected org.scribe.model.Token getAccessToken()
		{
		return accessToken;
		}
	
	protected void usage()
		{
		System.err.println(" -h help; This screen.");
		System.err.println(" -secret <api_secret> (or will use the one stored in the preferences).");
		System.err.println(" -key <api_key> (or will use the one stored in the preferences).");
		System.err.println(" -proxyHost <host>.");
		System.err.println(" -proxyPort <port>.");
		return;
		}

	protected int parseArgument(String[] args,int optind) throws Exception
		{
		if(args[optind].equals("-secret") && optind+1 < args.length)
			{
			api_secret= args[++optind];
			return optind;
			}
		else if(args[optind].equals("-key") && optind+1 < args.length)
			{
			api_key= args[++optind];
			return optind;
			}
		else if(args[optind].equals("-proxyHost") && optind+1 < args.length)
			{
			System.setProperty("http.proxyHost", args[++optind]);
			return optind;
			}
		else if(args[optind].equals("-proxyPort") && optind+1 < args.length)
			{
			System.setProperty("http.proxyPort", args[++optind]);
			return optind;
			}
		return -1;
		}
	
	
	protected int parseArguments(String[] args) throws Exception
		{
		int optind=0;
		int next;
		while(optind< args.length)
			{
			if(args[optind].equals("-h") ||
			   args[optind].equals("-help") ||
			   args[optind].equals("--help"))
				{
				usage();
				System.exit(0);
				}
			else if((next=parseArgument(args,optind))!=-1)
				{
				optind=next;
				}
			else if(args[optind].equals("--"))
				{
				optind++;
				break;
				}
			else if(args[optind].startsWith("-"))
				{
				System.err.println("Unknown option "+args[optind]);
				return -1;
				}
			else 
				{
				break;
				}
			++optind;
			}
		return optind;
		}
	
	protected boolean isIgnoringPrefs()
		{
		return force_ignore_prefs;
		}
	
	protected Preferences getPreferences()
		{
		if(this.preferences==null)
			{
			this.preferences=Preferences.userNodeForPackage(getClass());
			}
		return this.preferences;
		}
	
	protected String prefApiKey()
		{
		return getPreferencesPrefix()+".api.key";
		}
	protected String prefApiSecret()
		{
		return getPreferencesPrefix()+".api.secret";
		}

	protected String prefAccessToken()
		{
		return getPreferencesPrefix()+".access.token";
		}

	protected String prefAccessSecret()
		{
		return getPreferencesPrefix()+".access.secret";
		}
	protected String prefAccessRaw()
		{
		return getPreferencesPrefix()+".access.raw";
		}
	
	protected abstract Class<? extends Api> getApiClass();
	
	
	protected String askToken(String www)
		{
		System.out.println("Authorize the application here:\n\t"+ www +"\n");
    	if( Desktop.isDesktopSupported())
    		{
    		java.awt.Desktop desktop=java.awt.Desktop.getDesktop();
    		if(desktop.isSupported( java.awt.Desktop.Action.BROWSE ))
    			{
    			try
	    			{
	                java.net.URI uri = new java.net.URI( www );
	                System.err.println(uri);
	                desktop.browse( uri );
	    			}
    			catch(Exception err)
    				{
    				LOG.info("Desktop.error:"+err.getMessage());
    				}
    			}
    		else
    			{
    			LOG.info("Desktop is not supported");
    			}
    		}
    	
    	try
    		{
    		return JOptionPane.showInputDialog(null, www);
    		}
    	catch(Exception err)
    		{
    		LOG.info("GUI error: "+err);
    		}
    	Console console=System.console();
    	String token=null;
    	if( console!=null )
    		{
    		char pass[]=console.readPassword("Enter access token: ");
    		token=new String(pass);
    		}
    	else
    		{
	    	Scanner in = new Scanner(System.in);
	    	token=in.nextLine();
	    	in.close();
	    	
    		}
    	if(token==null || token.trim().isEmpty())
    		{
    		LOG.info("no token");
    		return null;
    		}
    	return token;
		}
	
	protected void connect()
		{
		if(api_key==null)
			{
			api_key=(isIgnoringPrefs()?null:getPreferences().get(prefApiKey(),null));
			if(api_key==null)
				{
				System.err.println("Undefined api_key");
				System.exit(-1);
				}
			}
		if(api_secret==null)
			{
			api_secret=(force_ignore_prefs?null:getPreferences().get(prefApiSecret(),null));
			if(api_secret==null)
				{
				System.err.println("Undefined api_secret");
				System.exit(-1);
				}
			}
		LOG.info("api_key:"+api_key);
		LOG.info("api_secret:"+api_secret);
		 this.service = new ServiceBuilder()
	        .provider(getApiClass())
	        .apiKey(api_key)
	        .apiSecret(api_secret)
	        .build();
	        
	        
	        org.scribe.model.Token requestToken=null;
	        if("A".equals("B"))
	        {
		requestToken  =  this.service.getRequestToken();
		 LOG.info("got request token");
		 
		 this.accessToken=null;
		 if(!(this.force_manual_access || force_ignore_prefs))
			 {
			 String access_token=getPreferences().get(prefAccessToken(), null);
			 String access_secret=getPreferences().get(prefAccessSecret(), null);
			 String access_raw=getPreferences().get(prefAccessRaw(), null);
	    	 if(access_token!=null && access_secret!=null)
	    		{
	    		this.accessToken=new org.scribe.model.Token(
	    				access_token,
	    				access_secret,
	    				access_raw
	    				);
	    		}
		     }
		     }
		    
	    if(this.accessToken==null || accessToken.isEmpty())
	    	{
	    	String www=this.service.getAuthorizationUrl(requestToken);
	    	LOG.info("confirm url:"+www);
	    	String token=askToken(www);
	    	if(token==null)
	    		{
	    		System.err.println("Error");
	    		System.exit(-1);
	    		}
	    	Verifier verifier = new Verifier(token.trim());
	    	
	    	this.accessToken = service.getAccessToken(requestToken, verifier);
	    	LOG.info("got AccessToken");
	    	}
		}
	
	protected void savePreferences()
		throws BackingStoreException
		{
		getPreferences().put(prefApiKey(), api_key);
		getPreferences().put(prefApiSecret(), api_secret);
		getPreferences().put(prefAccessToken(), getAccessToken().getToken());
		getPreferences().put(prefAccessSecret(),getAccessToken().getSecret());
		if(getAccessToken().getRawResponse()!=null)
			{
			getPreferences().put(prefAccessRaw(), getAccessToken().getRawResponse());
			}
		getPreferences().sync();
		}
	}
