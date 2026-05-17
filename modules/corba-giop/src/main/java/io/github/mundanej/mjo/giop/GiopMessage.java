package io.github.mundanej.mjo.giop;

/** Marker interface for supported GIOP message values. */
public sealed interface GiopMessage
    permits GiopRequest,
        GiopReply,
        GiopCancelRequest,
        GiopLocateRequest,
        GiopLocateReply,
        GiopCloseConnection,
        GiopMessageError,
        GiopFragment {

  /** Returns the fixed GIOP header associated with this message. */
  GiopHeader header();
}
