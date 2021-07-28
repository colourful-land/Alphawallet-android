package com.alphawallet.app.web3.entity;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.token.tools.Numeric;

import java.math.BigInteger;

/**
 * Created by JB on 28/07/21
 */

public class WalletAddEthereumChainObject
{
    public static class NativeCurrency {
        String name;
        String symbol;
        int decimals;
    }

    public NativeCurrency nativeCurrency;
    public String[] blockExplorerUrls;
    public String chainName;
    public String chainType; //ignore this
    public String chainId; //this is a hex number with "0x" prefix. If it is without "0x", process it as dec
    public String[] rpcUrls;

    public int getChainId()
    {
        try
        {
            if (Numeric.containsHexPrefix(chainId))
            {
                return Numeric.toBigInt(chainId).intValue();
            }
            else
            {
                return new BigInteger(chainId).intValue();
            }
        }
        catch (NumberFormatException e)
        {
            if (BuildConfig.DEBUG) e.printStackTrace();
            return (0);
        }
    }
}
