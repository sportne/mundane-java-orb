package io.github.mundanej.mjo.iiop;

import io.github.mundanej.mjo.orb.LocalInvocationUserException;
import io.github.mundanej.mjo.typecode.IdlOperationDescriptor;
import java.util.List;

/** Operation-specific CDR body codec used by the network ORB dispatch bridge. */
public interface IiopInvocationCodec {

  /** Decodes operation arguments from a GIOP request body. */
  List<Object> decodeArguments(IdlOperationDescriptor operation, byte[] requestBody);

  /** Encodes operation arguments into a GIOP request body. */
  byte[] encodeArguments(IdlOperationDescriptor operation, List<Object> arguments);

  /** Encodes a normal operation result into a GIOP reply body. */
  byte[] encodeReturnValue(IdlOperationDescriptor operation, Object value);

  /** Decodes a normal operation result from a GIOP reply body. */
  Object decodeReturnValue(IdlOperationDescriptor operation, byte[] replyBody);

  /** Encodes a declared user exception into a GIOP user-exception payload. */
  byte[] encodeUserException(LocalInvocationUserException exception);

  /** Decodes a declared user-exception reply for a generated-stub-facing client. */
  RuntimeException decodeUserException(
      IdlOperationDescriptor operation, String repositoryId, byte[] exceptionBody);
}
