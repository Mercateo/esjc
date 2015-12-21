package lt.msemys.esjc.node.static_;

import lt.msemys.esjc.node.EndPointDiscoverer;
import lt.msemys.esjc.node.NodeEndPoints;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

import static lt.msemys.esjc.util.Preconditions.checkNotNull;

public class StaticEndPointDiscoverer implements EndPointDiscoverer {

    private final CompletableFuture<NodeEndPoints> result;

    public StaticEndPointDiscoverer(StaticNodeSettings settings) {
        checkNotNull(settings, "settings is null");
        result = CompletableFuture.completedFuture(new NodeEndPoints(
            settings.ssl ? null : settings.address,
            settings.ssl ? settings.address : null));
    }

    @Override
    public CompletableFuture<NodeEndPoints> discover(InetSocketAddress failedTcpEndPoint) {
        return result;
    }

}
