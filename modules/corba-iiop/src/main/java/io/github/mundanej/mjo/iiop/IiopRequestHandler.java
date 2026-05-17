package io.github.mundanej.mjo.iiop;

import io.github.mundanej.mjo.giop.GiopReply;
import io.github.mundanej.mjo.giop.GiopRequest;

/** Handles one decoded GIOP request on an accepted local IIOP connection. */
@FunctionalInterface
public interface IiopRequestHandler {

  /** Returns the reply for a decoded request. */
  GiopReply handle(GiopRequest request) throws Exception;
}
