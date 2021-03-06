package org.swellrt.beta.model.remote;

import org.swellrt.beta.model.SText;
import org.waveprotocol.wave.model.wave.Blip;

/**
 * Abstract base class for remote text types which are platform
 * dependent, so instance creation must be delegated to {@see PlatformBasedFactory} 
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public abstract class STextRemote extends SNodeRemote implements SText {

  /**
   * A Blip is the original Wave representation of a Text document.
   * We keep using the Blip type as convenience as long as it matches 
   * quite well the interface SwellRT requires.
   * <p>
   * Blip is also platform independent unlike ContentDocument that is a
   * specific wrapper of a Blip for Web rendering.   
   */
  private final Blip blip;
  
  protected STextRemote(SObjectRemote object, SubstrateId substrateId, Blip blip) {
    super(substrateId, object);
    this.blip = blip;
  }
  
}
