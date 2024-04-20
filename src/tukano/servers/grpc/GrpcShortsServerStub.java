package tukano.servers.grpc;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import tukano.api.java.Shorts;
import tukano.api.java.Result;
import tukano.impl.grpc.generated_java.ShortsGrpc;
import tukano.impl.grpc.generated_java.ShortsProtoBuf.*;
import tukano.servers.java.JavaShorts;

import static tukano.impl.grpc.common.DataModelAdaptor.Short_to_GrpcShort;

public class GrpcShortsServerStub implements ShortsGrpc.AsyncService, BindableService {

    Shorts impl = new JavaShorts();

    @Override
    public ServerServiceDefinition bindService() {
        return ShortsGrpc.bindService(this);
    }

    @Override
    public void createShort(CreateShortArgs request, StreamObserver<CreateShortResult> responseObserver) {
        String userId = request.getUserId();
        String password = request.getPassword();

        var res = impl.createShort(userId, password);

        if(!res.isOK()) {
            responseObserver.onError(errorCodeToStatus(res.error()));
        } else {
            responseObserver.onNext( CreateShortResult.newBuilder().setValue( Short_to_GrpcShort( res.value() )).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void deleteShort(DeleteShortArgs request, StreamObserver<DeleteShortResult> responseObserver) {
        String shortId = request.getShortId();
        String password = request.getPassword();

        var res = impl.deleteShort(shortId, password);

        if(!res.isOK()) {
            responseObserver.onError(errorCodeToStatus(res.error()));
        } else {
            responseObserver.onNext(DeleteShortResult.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getShort(GetShortArgs request, StreamObserver<GetShortResult> responseObserver) {
        String shortId = request.getShortId();

        var res = impl.getShort(shortId);

        if(!res.isOK()) {
            responseObserver.onError(errorCodeToStatus(res.error()));
        } else {
            responseObserver.onNext(GetShortResult.newBuilder().setValue( Short_to_GrpcShort( res.value() )).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getShorts(GetShortsArgs request, StreamObserver<GetShortsResult> responseObserver) {
        String userId = request.getUserId();

        var res = impl.getShorts(userId);


        if(!res.isOK()) {
            responseObserver.onError(errorCodeToStatus(res.error()));
        } else {
            responseObserver.onNext(GetShortsResult.newBuilder().addAllShortId(res.value()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void follow(FollowArgs request, StreamObserver<FollowResult> responseObserver) {
        String userId1 = request.getUserId1();
        String userId2 = request.getUserId2();
        boolean isFollowing = request.getIsFollowing();
        String password = request.getPassword();

        var res = impl.follow(userId1, userId2, isFollowing, password);

        if(!res.isOK()) {
            responseObserver.onError(errorCodeToStatus(res.error()));
        } else {
            responseObserver.onNext(FollowResult.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void followers(FollowersArgs request, StreamObserver<FollowersResult> responseObserver) {
        String userId = request.getUserId();
        String password = request.getPassword();

        var res = impl.followers(userId, password);

        if(!res.isOK()) {
            responseObserver.onError(errorCodeToStatus(res.error()));
        } else {
            responseObserver.onNext(FollowersResult.newBuilder().addAllUserId(res.value()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void like(LikeArgs request, StreamObserver<LikeResult> responseObserver) {
        String shortId = request.getShortId();
        String userId = request.getUserId();
        boolean isLiked = request.getIsLiked();
        String password = request.getPassword();

        var res = impl.like(shortId, userId, isLiked, password);

        if(!res.isOK()) {
            responseObserver.onError(errorCodeToStatus(res.error()));
        } else {
            responseObserver.onNext(LikeResult.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void likes(LikesArgs request, StreamObserver<LikesResult> responseObserver) {
        String shortId = request.getShortId();
        String password = request.getPassword();

        var res = impl.likes(shortId, password);

        if(!res.isOK()) {
            responseObserver.onError(errorCodeToStatus(res.error()));
        } else {
            responseObserver.onNext(LikesResult.newBuilder().addAllUserId(res.value()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getFeed(GetFeedArgs request, StreamObserver<GetFeedResult> responseObserver) {
        String userId = request.getUserId();
        String password = request.getPassword();

        var res = impl.getFeed(userId, password);

        if(!res.isOK()) {
            responseObserver.onError(errorCodeToStatus(res.error()));
        } else {
            responseObserver.onNext(GetFeedResult.newBuilder().addAllShortId(res.value()).build());
            responseObserver.onCompleted();

        }
    }

    @Override
    public void deleteUser(DeleteUserArgs request, StreamObserver<DeleteUserResult> responseObserver) {
        String userId = request.getUserId();

        var res = impl.deleteUser(userId);

        if(!res.isOK()) {
            responseObserver.onError(errorCodeToStatus(res.error()));
        } else {
            responseObserver.onNext(DeleteUserResult.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    protected static Throwable errorCodeToStatus( Result.ErrorCode error ) {
        var status =  switch( error) {
            case NOT_FOUND -> io.grpc.Status.NOT_FOUND;
            case CONFLICT -> io.grpc.Status.ALREADY_EXISTS;
            case FORBIDDEN -> io.grpc.Status.PERMISSION_DENIED;
            case NOT_IMPLEMENTED -> io.grpc.Status.UNIMPLEMENTED;
            case BAD_REQUEST -> io.grpc.Status.INVALID_ARGUMENT;
            default -> io.grpc.Status.INTERNAL;
        };

        return status.asException();
    }
}
