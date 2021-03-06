package org.swellrt.beta.client.operation.impl;

import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.operation.HTTPOperation;
import org.swellrt.beta.client.operation.Operation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.account.ServerAccountData;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public final class ResumeOperation extends HTTPOperation<ResumeOperation.Options, ResumeOperation.Response> {

  @JsType(isNative = true)
  public interface Options extends Operation.Options {

    @JsProperty
    public String getId();

    @JsProperty
    public String getIndex();

  }

  @JsType(isNative = true)
  public interface Response extends Operation.Response, ServerAccountData {


  }


  public ResumeOperation(ServiceContext context) {
    super(context);
  }


  @Override
  protected void onError(Throwable exception, Callback<Response> callback) {
    if (callback != null)
      callback.onError(new SException(SException.OPERATION_EXCEPTION, exception));
  }


  @Override
  protected void onSuccess(int statusCode, String data, Callback<Response> callback) {
    Response response = generateResponse(data);
    getServiceContext().init(response);

    if (callback != null)
      callback.onSuccess(response);
  }


  @Override
  public void execute(Options options, Callback<Response> callback) {
    addPathElement("auth");

    setBody(generateBody(options));

    executePost(callback);
  }



}
