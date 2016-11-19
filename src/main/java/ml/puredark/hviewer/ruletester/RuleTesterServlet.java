package ml.puredark.hviewer.ruletester;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import okhttp3.FormBody;
import okhttp3.RequestBody;
import ml.puredark.hviewer.ruletester.beans.Collection;
import ml.puredark.hviewer.ruletester.beans.Rule;
import ml.puredark.hviewer.ruletester.beans.Site;
import ml.puredark.hviewer.ruletester.http.HViewerHttpClient;
import ml.puredark.hviewer.ruletester.http.Logger;
import ml.puredark.hviewer.ruletester.utils.Base64Util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.RequestHeaders;
import com.machinepublishers.jbrowserdriver.Settings;
import com.machinepublishers.jbrowserdriver.Timezone;

public class RuleTesterServlet extends HttpServlet {

	public RuleTesterServlet() {
		super();

        // 设置Json默认配置
        Configuration.setDefaults(new Configuration.Defaults() {
            private final JsonProvider jsonProvider = new GsonJsonProvider();
            private final MappingProvider mappingProvider = new GsonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                Set<Option> options = EnumSet.noneOf(Option.class);
                options.add(Option.DEFAULT_PATH_LEAF_TO_NULL);
                return options;
            }
        });
	}
	
	@Override
	public void destroy() {
		super.destroy(); 
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		String action = request.getParameter("action");
		String siteJson = request.getParameter("site");
        try {
			if("getList".equals(action)){
				String targetUrl = request.getParameter("targetUrl");
				if(!TextUtils.isEmpty(siteJson) && !TextUtils.isEmpty(targetUrl)){
					Gson gson = new Gson();
					Site site = gson.fromJson(siteJson, Site.class);
					List<Collection> collections = getCollections(site, targetUrl);
					String output = gson.toJson(collections);
					out.println(output);
				}
			}else if("getDetail".equals(action)){
				String collectionJson = request.getParameter("collection");
				if(!TextUtils.isEmpty(collectionJson) && !TextUtils.isEmpty(collectionJson)){
					Gson gson = new Gson();
					Site site = gson.fromJson(siteJson, Site.class);
					Collection collection = gson.fromJson(collectionJson, Collection.class);
					collection = getCollectionDetail(site, collection);
					String output = gson.toJson(collection);
					out.println(output);
				}
			}else if("generateQrCode".equals(action)){
	
//		        RequestBody requestBody = new FormBody.Builder()
//		                .add("key", PasteEEConfig.appkey)
//		                .add("description", "")
//		                .add("paste", siteJson)
//		                .add("format", "json")
//		                .build();
//
//		        String result = HViewerHttpClient.post(PasteEEConfig.url, requestBody, null);
//		        String url = null;
//	                JsonObject jsonObject = new JsonParser().parse((String) result).getAsJsonObject();
//	                if (jsonObject.has("status") && "success".equals(jsonObject.get("status").getAsString())) {
//	                    url = jsonObject.get("paste").getAsJsonObject().get("raw").getAsString();
//						BufferedImage image = QRCodeUtil.createImage(url, null, false);
//						ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
//			            boolean flag = ImageIO.write(image, "png", byteOut);
//			            byte[] bytes = byteOut.toByteArray();
//						String base64 = Base64Util.getImageStr(bytes);
//						out.println(base64);
//	                }
			}
        } catch (Exception e) {
            e.printStackTrace();
            out.println(e.getMessage());
        }
		
		out.flush();
		out.close();
	}
	
	private List<Collection> getCollections(Site site, String targetUrl) {
        final Rule rule = site.indexRule;
        final String url = targetUrl;
        Logger.d("getCollections", url);
        String html = "";
        if (site.hasFlag(Site.FLAG_JS_NEEDED_ALL) || site.hasFlag(Site.FLAG_JS_NEEDED_INDEX)){
            Logger.d("getCollections", "browser");
        	html = getHtmlWithBrowser(targetUrl, site.cookie);
        }else if (site.hasFlag(Site.FLAG_POST_ALL) || site.hasFlag(Site.FLAG_POST_INDEX)){
            String params = (url == null) ? "" : url.substring(url.indexOf('?'));
            Logger.d("getCollections", "post");
        	html = HViewerHttpClient.post(url, params, site.cookie);
        }else {
            Logger.d("getCollections", "get");
        	html = HViewerHttpClient.get(url, site.cookie);
        }
        Logger.d("getCollections", "result:"+html);
        List<Collection> collections = new ArrayList<Collection>();
        collections = RuleParser.getCollections(collections, html, rule, url);
        return collections;
    }
	
	private Collection getCollectionDetail(Site site, Collection collection) {
        final String url = site.getGalleryUrl(collection.idCode, 1, collection.pictures);
        Logger.d("getCollectionDetail", url);
        String html = "";
        if (site.hasFlag(Site.FLAG_JS_NEEDED_ALL) || site.hasFlag(Site.FLAG_JS_NEEDED_GALLERY)){
            Logger.d("getCollections", "browser");
        	html = getHtmlWithBrowser(url, site.cookie);
        }else if (site.hasFlag(Site.FLAG_POST_ALL) || site.hasFlag(Site.FLAG_POST_INDEX)){
            String params = (url == null) ? "" : url.substring(url.indexOf('?'));
        	html = HViewerHttpClient.post(url, params, site.cookie);
        }else {
        	html = HViewerHttpClient.get(url, site.cookie);
        }
        Logger.d("getCollectionDetail", html);
        collection = RuleParser.getCollectionDetail(collection, html, site.galleryRule, url);
        return collection;
    }
	
	private String getHtmlWithBrowser(String url, String cookie){
		LinkedHashMap<String, String> headers = new LinkedHashMap<String, String>();
		headers.put("cookie", cookie);
		
	    JBrowserDriver driver = new JBrowserDriver(
	    		Settings.builder()
	    		.timezone(Timezone.ASIA_SHANGHAI)
	    		.ajaxResourceTimeout(15000)
	    		.headless(true)
	    		.blockAds(true)
	    		.requestHeaders(new RequestHeaders(headers))
	    		.build()
	    );

	    driver.get(url);

	    System.out.println(driver.getStatusCode());
	    
	    String html = driver.getPageSource();
//	    System.out.println(driver.getPageSource());

	    driver.quit();
	    
	    return html;
	}


}
