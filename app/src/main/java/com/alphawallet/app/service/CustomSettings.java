package com.alphawallet.app.service;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.content.Context;
import android.util.LongSparseArray;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CustomSettings
{
    public final String CUSTOM_SETTINGS_FILENAME = "custom_view_settings.json";
    public static final long primaryChain = MAINNET_ID;
    private final Context context;
    ConcurrentLinkedQueue<Long> loadLockedCachedChains = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<Long> loadExclusiveCachedChains = new ConcurrentLinkedQueue<>();
    private final LongSparseArray<Map<String, TokenInfo>> loadLockedTokens = new LongSparseArray<>();
    public String getJsonString = "";
    public Boolean loaded = false;

    public CustomSettings(Context context)
    {
        this.context = context;
    }

    public JSONArray loadDataFromJsonArray(String chainName)
    {
        JSONArray jsonArray = new JSONArray();
        try
        {

            String lockedChains = loadJSONStringFromAsset();
            if (lockedChains != null)
            {
                JSONObject customSettingsJsonObject = new JSONObject(lockedChains);
                jsonArray = customSettingsJsonObject.getJSONArray(chainName);
            }
        }
        catch (JSONException err)
        {
            err.printStackTrace();
        }

        return jsonArray;
    }

    //TODO: Cache locked chains & Tokens in an ConcurrentLinkedQueue / ConcurrentHashMap
    //    : The approach I would take is to have a 'load' function which first checks if there's already
    //    : data in the two cache mappings, if there's not then check the 'loaded' flag. If that is
    //    : set false then load and populate the Queue/Map
    //    : then, just use the mappings from there. Add a call to the load function each time you use the data.
    //    : Android can memory scavenge those mappings at any time if the wallet is paged out,
    //    : Then they'll be empty when the wallet is paged back in.

    public ArrayList<Long> getChainsFromJsonFile() //<-- TODO: chainName is redundant
    {
        ArrayList<Long> chains = new ArrayList<>();
        if (loadLockedCachedChains.size() <= 0)
        {
            JSONArray chainsArray = loadDataFromJsonArray("locked_chains");
            if (chainsArray.length() > 0)
            {
                try
                {
                    for (int i = 0; i < chainsArray.length(); i++)
                    {
                        JSONObject chainObject = chainsArray.getJSONObject(i);
                        Long chain = chainObject.getLong("chain");
                        chains.add(chain);
                    }
                }
                catch (JSONException err)
                {
                    err.printStackTrace();
                }
                loadLockedCachedChains.clear();
                loadLockedCachedChains.addAll(chains);
            }
        }
        chains.addAll(loadLockedCachedChains);
        return chains;
    }

    public ConcurrentHashMap<String, TokenInfo> getLockedTokensFromJsonFile() //<-- TODO: chainName is redundant
    {
        ConcurrentHashMap<String, TokenInfo> chains = new ConcurrentHashMap<>();
        Gson gson = new Gson();
        JSONArray chainsArray = loadDataFromJsonArray("locked_tokens");
        if (chainsArray.length() > 0)
        {
            loadLockedTokens.clear();
            //TODO: use GSON class array load (see "private EtherscanTransaction[] getEtherscanTransactions" for how-to)
            //    : this will need a static class within this class
            //    : you can then traverse (for x : y) that list and have cleaner code
            //    : esp if you add a getTokenInfo from that static internal class.
            TokenInfo[] lockedTokenInfo = gson.fromJson(chainsArray.toString(), TokenInfo[].class);
            int i = 0;
            for (TokenInfo tokenInfo : lockedTokenInfo)
            {
                chains.put(String.valueOf(i), tokenInfo);
                loadLockedTokens.put(tokenInfo.chainId, chains);
                i++;
            }
        }

        return chains;
    }

    public JSONArray getChainsArrayJsonFile() //<--- TODO: Redundant
    {
        JSONArray chainsArray = new JSONArray();
        try
        {
            String lockedChains = loadJSONStringFromAsset();
            JSONObject customSettingsJsonObject = new JSONObject(lockedChains);
            chainsArray = customSettingsJsonObject.getJSONArray("locked_chains");
        }
        catch (JSONException err)
        {
            err.printStackTrace();
        }

        return chainsArray;
    }

    //TODO: Doesn't need caching (no action)
    public Boolean getDarkModeValueFromJsonFile(String chainName)
    {
        boolean darkModeValue = false;
        try
        {
            String darkMode = loadJSONStringFromAsset();
            if (darkMode != null)
            {
                JSONObject customSettingsJsonObject = new JSONObject(darkMode);
                darkModeValue = customSettingsJsonObject.getBoolean("dark_mode");
            }
        }
        catch (JSONException err)
        {
            err.printStackTrace();
        }
        return darkModeValue;
    }

    // Function Use:  Universal function to check if Json is already loaded or not. If it is already loaded then we will return the value directly from the variable else we will load the json data from asset.
    public String loadJSONStringFromAsset()
    {
        try
        {
            if (getJsonString.isEmpty())
            {
                Reader reader = new InputStreamReader(context.getAssets().open(CUSTOM_SETTINGS_FILENAME));
                JsonElement json = new Gson().fromJson(reader, JsonElement.class);
                getJsonString = json.toString();
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            return null;
        }

        return getJsonString;
    }


    //TODO: Caching
    public Boolean alwaysShow(long chainId)
    {
        ArrayList<Long> exclusiveChains = new ArrayList<>();
        if (loadExclusiveCachedChains.size() <= 0)
        {
            JSONArray chainsArray = loadDataFromJsonArray("exclusive_chains");
            if (chainsArray.length() > 0)
            {
                try
                {
                    for (int i = 0; i < chainsArray.length(); i++)
                    {
                        JSONObject chainObject = chainsArray.getJSONObject(i);
                        Long chain = chainObject.getLong("chain");
                        exclusiveChains.add(chain);
                    }
                }
                catch (JSONException err)
                {
                    err.printStackTrace();
                }
                loadExclusiveCachedChains.clear();
                loadExclusiveCachedChains.addAll(exclusiveChains);

            }
        }
        exclusiveChains.addAll(loadExclusiveCachedChains);
        return exclusiveChains.contains(chainId);
    }

    //TODO: Requires caching, since this will be called very frequently
    //    : Use a (final) mapping of locked tokens, from a load.
    //    : You'll need to check if the list is empty and if so flag a 'loaded', so we don't spam this list
    public Boolean tokenCanBeDisplayed(TokenCardMeta token)
    {
        final ArrayList<TokenInfo> lockedTokens = new ArrayList<>();
        if (loadLockedTokens.size() > 0)
        {
            return lockedTokens.addAll(loadLockedTokens.get(primaryChain).values());
        }
        else
        {
            return token.type == ContractType.ETHEREUM || token.isEnabled || isLockedToken(token.getChain(), token.getAddress());
        }

    }

    //TODO: Caching
    public Boolean isLockedToken(long chainId, String contractAddress)
    {
        final ConcurrentHashMap<String, TokenInfo> lockedTokens = getLockedTokensFromJsonFile();
        Map<String, TokenInfo> mapping = loadLockedTokens.get(chainId);
        return mapping.containsKey(contractAddress.toLowerCase(Locale.ROOT));
    }

    public ContractType checkKnownTokens(TokenInfo tokenInfo)
    {
        return ContractType.OTHER;
    }

    public boolean showContractAddress(Token token)
    {
        return getBooleanValueFromJson(false, "isShowContractAddress");
    }

    public long startupDelay()
    {
        long startupDelayValue = 0;
        try
        {
            String startupDelay = loadJSONStringFromAsset();
            if (startupDelay != null)
            {
                JSONObject customSettingsJsonObject = new JSONObject(startupDelay);
                startupDelayValue = customSettingsJsonObject.getLong("startupDelay");
            }
        }
        catch (JSONException err)
        {
            err.printStackTrace();
        }
        return startupDelayValue;
    }

    public int getImageOverride()
    {
        return getIntegerValueFromJson(0, "imageOverride");
    }

    //Switch off dapp browser
    public boolean hideDappBrowser()
    {
        return getBooleanValueFromJson(false, "isHideDappBrowser");
    }

    //Hides the filter tab bar at the top of the wallet screen (ALL/CURRENCY/COLLECTIBLES)
    public boolean hideTabBar()
    {
        return getBooleanValueFromJson(false, "isHideTabBar");
    }

    //Use to switch off direct transfer, only use magiclink transfer
    public boolean hasDirectTransfer()
    {
        return getBooleanValueFromJson(true, "isHasDirectTransfer");
    }

    //Allow multiple wallets (true) or single wallet mode (false)
    public boolean canChangeWallets()
    {
        return getBooleanValueFromJson(true, "isCanChangeWallets");
    }

    //Hide EIP681 generation (Payment request, generates a QR code another wallet user can scan to have all payment fields filled in)
    public boolean hideEIP681()
    {
        return getBooleanValueFromJson(false, "isHideEIP681");
    }

    //In main wallet menu, if wallet allows adding new tokens
    public boolean canAddTokens()
    {
        return getBooleanValueFromJson(true, "isCanAddTokens");
    }

    //Implement minimal dappbrowser with no URL bar. You may want this if you want your browser to point to a specific website and only
    // allow navigation within that website
    // use this setting in conjunction with changing DEFAULT_HOMEPAGE in class EthereumNetworkBase
    //Implement minimal dappbrowser with no URL bar. You may want this if you want your browser to point to a specific website and only
    // allow navigation within that website
    // use this setting in conjunction with changing DEFAULT_HOMEPAGE in class EthereumNetworkBase
    public static boolean minimiseBrowserURLBar()
    {
        return false;
    }

    //Allow showing token management view
    public boolean showManageTokens()
    {
        return getBooleanValueFromJson(true, "isShowManageTokens");
    }

    //Show all networks in Select Network screen. Set to `true` to show only filtered networks.
    public boolean showAllNetworks()
    {
        return getBooleanValueFromJson(false, "isShowAllNetworks");
    }

    public String getDecimalFormat()
    {
        return "0.####E0";
    }

    public int getDecimalPlaces()
    {
        return getIntegerValueFromJson(5, "getDecimalPlaces");
    }

    //set if the Input Amount defaults to Fiat or Crypto
    public static boolean inputAmountFiatDefault()
    {
        return false;
    }

    //Common function for returning the boolean value
    public boolean getBooleanValueFromJson(boolean variableName, String jsonString)
    {
        try
        {
            String isHideDappBrowser = loadJSONStringFromAsset();
            if (isHideDappBrowser != null)
            {
                JSONObject customSettingsJsonObject = new JSONObject(isHideDappBrowser);
                variableName = customSettingsJsonObject.getBoolean(jsonString);
            }
        }
        catch (JSONException err)
        {
            err.printStackTrace();
        }
        return variableName;
    }

    //Common function for returning the integer value
    public int getIntegerValueFromJson(int jsonValue, String jsonString)
    {
        int integerValue = jsonValue;
        try
        {
            String imageOverride = loadJSONStringFromAsset();
            if (imageOverride != null)
            {
                JSONObject customSettingsJsonObject = new JSONObject(imageOverride);
                integerValue = customSettingsJsonObject.getInt(jsonString);
            }
        }
        catch (JSONException err)
        {
            err.printStackTrace();
        }
        return integerValue;
    }
}
