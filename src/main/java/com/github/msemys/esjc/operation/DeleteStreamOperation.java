package com.github.msemys.esjc.operation;

import com.github.msemys.esjc.DeleteResult;
import com.github.msemys.esjc.Position;
import com.github.msemys.esjc.UserCredentials;
import com.github.msemys.esjc.proto.EventStoreClientMessages.DeleteStreamCompleted;
import com.github.msemys.esjc.tcp.TcpCommand;
import com.google.protobuf.MessageLite;

import java.util.concurrent.CompletableFuture;

import static com.github.msemys.esjc.proto.EventStoreClientMessages.DeleteStream;

public class DeleteStreamOperation extends AbstractOperation<DeleteResult, DeleteStreamCompleted> {

    private final boolean requireMaster;
    private final String stream;
    private final long expectedVersion;
    private final boolean hardDelete;

    public DeleteStreamOperation(CompletableFuture<DeleteResult> result,
                                 boolean requireMaster,
                                 String stream,
                                 long expectedVersion,
                                 boolean hardDelete,
                                 UserCredentials userCredentials) {
        super(result, TcpCommand.DeleteStream, TcpCommand.DeleteStreamCompleted, userCredentials);
        this.requireMaster = requireMaster;
        this.stream = stream;
        this.expectedVersion = expectedVersion;
        this.hardDelete = hardDelete;
    }

    @Override
    protected MessageLite createRequestMessage() {
        return DeleteStream.newBuilder()
            .setEventStreamId(stream)
            .setExpectedVersion(expectedVersion)
            .setRequireMaster(requireMaster)
            .setHardDelete(hardDelete)
            .build();
    }

    @Override
    protected DeleteStreamCompleted createResponseMessage() {
        return DeleteStreamCompleted.getDefaultInstance();
    }

    @Override
    protected InspectionResult inspectResponseMessage(DeleteStreamCompleted response) {
        switch (response.getResult()) {
            case Success:
                succeed();
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("Success")
                    .build();
            case PrepareTimeout:
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.Retry)
                    .description("PrepareTimeout")
                    .build();
            case CommitTimeout:
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.Retry)
                    .description("CommitTimeout")
                    .build();
            case ForwardTimeout:
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.Retry)
                    .description("ForwardTimeout")
                    .build();
            case WrongExpectedVersion:
                fail(new WrongExpectedVersionException("Delete stream failed due to WrongExpectedVersion. Stream: %s, Expected version: %d.", stream, expectedVersion));
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("WrongExpectedVersion")
                    .build();
            case StreamDeleted:
                fail(new StreamDeletedException(stream));
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("StreamDeleted")
                    .build();
            case InvalidTransaction:
                fail(new InvalidTransactionException());
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("InvalidTransaction")
                    .build();
            case AccessDenied:
                fail(new AccessDeniedException(String.format("Write access denied for stream '%s'.", stream)));
                return InspectionResult.newBuilder()
                    .decision(InspectionDecision.EndOperation)
                    .description("AccessDenied")
                    .build();
            default:
                throw new IllegalArgumentException(String.format("Unexpected OperationResult: %s.", response.getResult()));
        }
    }

    @Override
    protected DeleteResult transformResponseMessage(DeleteStreamCompleted response) {
        long commitPosition = response.hasCommitPosition() ? response.getCommitPosition() : -1;
        long preparePosition = response.hasPreparePosition() ? response.getPreparePosition() : -1;
        return new DeleteResult(new Position(commitPosition, preparePosition));
    }

    @Override
    public String toString() {
        return String.format("Stream: %s, ExpectedVersion: %d.", stream, expectedVersion);
    }
}
