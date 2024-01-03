package com.codeard.yandexmetrica;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;

import java.lang.Exception;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Currency;

import org.json.JSONObject;
import org.json.JSONArray;

import com.yandex.metrica.YandexMetrica;
import com.yandex.metrica.YandexMetricaConfig;
import com.yandex.metrica.ecommerce.ECommerceAmount;
import com.yandex.metrica.ecommerce.ECommerceCartItem;
import com.yandex.metrica.ecommerce.ECommerceEvent;
import com.yandex.metrica.ecommerce.ECommerceOrder;
import com.yandex.metrica.ecommerce.ECommercePrice;
import com.yandex.metrica.ecommerce.ECommerceProduct;
import com.yandex.metrica.ecommerce.ECommerceReferrer;
import com.yandex.metrica.ecommerce.ECommerceScreen;
import com.yandex.metrica.profile.UserProfile;
import com.yandex.metrica.profile.Attribute;
import com.yandex.metrica.Revenue;

import java.lang.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class YandexAppmetricaModule extends ReactContextBaseJavaModule {

    private static String TAG = "YandexAppmetrica";

    private final ReactApplicationContext reactContext;

    public YandexAppmetricaModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return TAG;
    }

    @ReactMethod
    public void activateWithApiKey(String apiKey) {
        YandexMetricaConfig.Builder configBuilder = YandexMetricaConfig.newConfigBuilder(apiKey).withLogs();
        YandexMetrica.activate(getReactApplicationContext().getApplicationContext(), configBuilder.build());
        Activity activity = getCurrentActivity();
        if (activity != null) {
            Application application = activity.getApplication();
            YandexMetrica.enableActivityAutoTracking(application);
        }
    }

    @ReactMethod
    public void activateWithConfig(ReadableMap params) {
        YandexMetricaConfig.Builder configBuilder = YandexMetricaConfig.newConfigBuilder(params.getString("apiKey")).withLogs();
        if (params.hasKey("sessionTimeout")) {
            configBuilder.withSessionTimeout(params.getInt("sessionTimeout"));
        }
        if (params.hasKey("firstActivationAsUpdate")) {
            configBuilder.handleFirstActivationAsUpdate(params.getBoolean("firstActivationAsUpdate"));
        }
        YandexMetrica.activate(getReactApplicationContext().getApplicationContext(), configBuilder.build());
        Activity activity = getCurrentActivity();
        if (activity != null) {
            Application application = activity.getApplication();
            YandexMetrica.enableActivityAutoTracking(application);
        }
    }

    @ReactMethod
    public void reportEvent(String message, @Nullable ReadableMap params) {
      try {
          if (params != null) {
              YandexMetrica.reportEvent(message, convertMapToJson(params).toString());
          } else {
              YandexMetrica.reportEvent(message);
          }
      } catch (Exception e) {
          Log.e(TAG, "Unable to report Yandex Mobile Metrica event: " + e);
      }
    }

    @ReactMethod
    public void reportError(@NonNull String message, @Nullable String exceptionError) {
        Throwable exception = null;
        if (exceptionError != null) {
            exception = new Throwable(exceptionError);
        }
        YandexMetrica.reportError(message, exception);
    }

    @ReactMethod
    public void reportError(@NonNull String message, @Nullable ReadableMap exceptionError) {
        Throwable exception = null;
        if (exceptionError != null) {
            exception = new Throwable(convertMapToJson(exceptionError).toString());
        }
        YandexMetrica.reportError(message, exception);
    }

    @ReactMethod
    public void reportRevenue(String productId, Double price, Integer quantity) {
        Revenue revenue = Revenue.newBuilder(price, Currency.getInstance("RUB"))
            .withProductID(productId)
            .withQuantity(quantity)
            .build();

        YandexMetrica.reportRevenue(revenue);
    }

    @ReactMethod
    public void setUserProfileID(String profileID) {
        YandexMetrica.setUserProfileID(profileID);
    }



    private JSONObject convertMapToJson(ReadableMap readableMap) {
        JSONObject object = new JSONObject();
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        try {
            while (iterator.hasNextKey()) {
                String key = iterator.nextKey();
                switch (readableMap.getType(key)) {
                    case Null:
                        object.put(key, JSONObject.NULL);
                        break;
                    case Boolean:
                        object.put(key, readableMap.getBoolean(key));
                        break;
                    case Number:
                        object.put(key, readableMap.getDouble(key));
                        break;
                    case String:
                        object.put(key, readableMap.getString(key));
                        break;
                    case Map:
                        object.put(key, convertMapToJson(readableMap.getMap(key)));
                        break;
                    case Array:
                        object.put(key, convertArrayToJson(readableMap.getArray(key)));
                        break;
                }
            }
        }
        catch (Exception ex) {
            Log.d(TAG, "convertMapToJson fail: " + ex);
        }
        return object;
    }

    private JSONArray convertArrayToJson(ReadableArray readableArray) {
        JSONArray array = new JSONArray();
        try {
            for (int i = 0; i < readableArray.size(); i++) {
                switch (readableArray.getType(i)) {
                    case Null:
                        break;
                    case Boolean:
                        array.put(readableArray.getBoolean(i));
                        break;
                    case Number:
                        array.put(readableArray.getDouble(i));
                        break;
                    case String:
                        array.put(readableArray.getString(i));
                        break;
                    case Map:
                        array.put(convertMapToJson(readableArray.getMap(i)));
                        break;
                    case Array:
                        array.put(convertArrayToJson(readableArray.getArray(i)));
                        break;
                }
            }
        }
        catch (Exception ex) {
            Log.d(TAG, "convertArrayToJson fail: " + ex);
        }
        return array;
    }

    @ReactMethod
    public void sendEventsBuffer() {
        YandexMetrica.sendEventsBuffer();
    }


    ///// E-commerce

    public ECommerceScreen createScreen(ReadableMap params) {
      if (params == null) return null;
      return new ECommerceScreen()
              .setName(params.getString("name"))
              .setCategoriesPath(Utils.toListString(params.getArray("categoryComponents")))
              .setSearchQuery(params.getString("searchQuery"))
              .setPayload(Utils.toMapString(params.getMap("payload")));
  }

  public ECommerceReferrer createReferrer(ReadableMap params) {
      if (params == null) return null;
      ECommerceScreen screen = this.createScreen(params.getMap("screen"));
      return new ECommerceReferrer()
              .setType(params.getString("type"))
              .setIdentifier(params.getString("identifier"))
              .setScreen(screen);
  }

  public ECommercePrice createPrice(ReadableMap params) {
      if (params == null) return null;
      ReadableMap fiatMap = params.getMap("fiat");
      ECommerceAmount fiat = new ECommerceAmount(fiatMap.getDouble("value"), fiatMap.getString("currency"));
      // TODO: internalComponents
      return new ECommercePrice(fiat);
  }

  public ECommerceProduct createProduct(ReadableMap params) {
      return new ECommerceProduct(params.getString("sku"))
              .setName(params.getString("name"))
              .setCategoriesPath(Utils.toListString(params.getArray("categoryComponents")))
              .setPayload(Utils.toMapString(params.getMap("payload")))
              .setActualPrice(this.createPrice(params.getMap("actualPrice")))
              .setOriginalPrice(this.createPrice(params.getMap("originalPrice")))
              .setPromocodes(Utils.toListString(params.getArray("promoCodes")));
  }

  public ECommerceCartItem createCartItem(ReadableMap params) {
      ECommerceProduct product = this.createProduct(params.getMap("product"));
      ECommercePrice revenue = this.createPrice(params.getMap("revenue"));
      ECommerceReferrer referrer = this.createReferrer(params.getMap("referrer"));

      return new ECommerceCartItem(product, revenue, params.getInt("quantity"))
              .setReferrer(referrer);
  }

  @ReactMethod
  public void showScreen(ReadableMap screenParams) {
      ECommerceScreen screen = this.createScreen(screenParams);
      ECommerceEvent showScreenEvent = ECommerceEvent.showScreenEvent(screen);
      YandexMetrica.reportECommerce(showScreenEvent);
  }

  @ReactMethod
  public void showProductCard(ReadableMap productParams, ReadableMap screenParams) {
      ECommerceProduct product = this.createProduct(productParams);
      ECommerceScreen screen = this.createScreen(screenParams);
      ECommerceEvent showProductCardEvent = ECommerceEvent.showProductCardEvent(product, screen);
      YandexMetrica.reportECommerce(showProductCardEvent);
  }

  @ReactMethod
  public void showProductDetails(ReadableMap productParams, ReadableMap referrerParams) {
      ECommerceProduct product = this.createProduct(productParams);
      ECommerceReferrer referrer = this.createReferrer(referrerParams);
      ECommerceEvent showProductDetailsEvent = ECommerceEvent.showProductDetailsEvent(product, referrer);
      YandexMetrica.reportECommerce(showProductDetailsEvent);
  }

  @ReactMethod
  public void addToCart(ReadableMap cartItemParams) {
      ECommerceCartItem cartItem = this.createCartItem(cartItemParams);
      ECommerceEvent addCartItemEvent = ECommerceEvent.addCartItemEvent(cartItem);
      YandexMetrica.reportECommerce(addCartItemEvent);
  }

  @ReactMethod
  public void removeFromCart(ReadableMap params) {
      ECommerceCartItem cartItem = this.createCartItem(params);
      ECommerceEvent removeCartItemEvent = ECommerceEvent.removeCartItemEvent(cartItem);
      YandexMetrica.reportECommerce(removeCartItemEvent);
  }

  @ReactMethod
  public void beginCheckout(ReadableArray cartItems, String identifier, ReadableMap payload) {
      ArrayList<ECommerceCartItem> cartItemsObj = new ArrayList<>();
      for (int i = 0; i < cartItemsObj.size(); i++) {
          cartItemsObj.add(this.createCartItem(cartItems.getMap(i)));
      }
      ECommerceOrder order = new ECommerceOrder(identifier, cartItemsObj)
              .setPayload(Utils.toMapString(payload));
      ECommerceEvent beginCheckoutEvent = ECommerceEvent.beginCheckoutEvent(order);
      YandexMetrica.reportECommerce(beginCheckoutEvent);
  }

  @ReactMethod
  public void purchase(ReadableArray cartItems, String identifier, ReadableMap payload) {
      ArrayList<ECommerceCartItem> cartItemsObj = new ArrayList<>();
      for (int i = 0; i < cartItemsObj.size(); i++) {
          cartItemsObj.add(this.createCartItem(cartItems.getMap(i)));
      }
      ECommerceOrder order = new ECommerceOrder(identifier, cartItemsObj)
              .setPayload(Utils.toMapString(payload));
      ECommerceEvent purchaseEvent = ECommerceEvent.purchaseEvent(order);
      YandexMetrica.reportECommerce(purchaseEvent);
  }

}
