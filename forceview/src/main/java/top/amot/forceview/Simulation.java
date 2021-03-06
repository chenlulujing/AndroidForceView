package top.amot.forceview;

import android.content.Context;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import androidx.collection.ArrayMap;

public final class Simulation {

    public interface Callback {
        void onTick();
        void onEnd();
    }

    private static final double INITIAL_RADIUS = 30;
    private static final double INITIAL_ANGLE = Math.PI * (3 - Math.sqrt(5));
    private static final int DEFAULT_ITERATIONS = 1;

    private double initialRadius;
    private double initialAngle;
    private double alpha;
    private double alphaMin;
    private double alphaDecay;
    private double alphaTarget;
    private double velocityDecay;
    private Map<String, Force> forces;
    private int iterations = DEFAULT_ITERATIONS;

    private Node[] nodes;
    private Link[] links;

    private Stepper stepper = new Stepper(this::step);
    private Callback callback;

    public Simulation setIterations(int i) {
        iterations = i;
        return this;
    }

    public Node[] getNodes() {
        return nodes;
    }

    public Link[] getLinks() {
        return links;
    }

    public double getAlpha() {
        return alpha;
    }

    public double getAlphaMin() {
        return alphaMin;
    }

    public double getAlphaDecay() {
        return alphaDecay;
    }

    public double getAlphaTarget() {
        return alphaTarget;
    }

    public double getVelocityDecay() {
        return 1 - velocityDecay;
    }

    public Force getForce(String name) {
        if (name != null) {
            return forces.get(name);
        }
        return null;
    }

    public void removeForce(String name) {
        if (name == null) {
            return;
        }
        forces.remove(name);
    }

    void setCallback(Callback callback) {
        this.callback = callback;
    }

    void restart() {
        stepper.restart();
    }

    void stop() {
        stepper.stop();
    }

    void destroy() {
        stepper.destroy();
    }

    Node find(double x, double y, double radius) {
        Node closest = null;
        double radius2;
        if (radius <= 0) {
            radius2 = Double.POSITIVE_INFINITY;
        } else {
            radius2 = radius * radius;
        }

        for (Node node : nodes) {
            double dx = x - node.x;
            double dy = y - node.y;
            double d2 = dx * dx + dy * dy;
            if (d2 < radius2) {
                closest = node;
                break;
            }
        }

        return closest;
    }

    private void step() {
        tick(iterations);
        if (callback != null) {
            callback.onTick();
        }
        if (alpha < alphaMin) {
            stepper.stop();
            if (callback != null) {
                callback.onEnd();
            }
        }
    }

    private void tick(int iterations) {
        if (iterations <= 0) {
            iterations = DEFAULT_ITERATIONS;
        }

        for (int k = 0; k < iterations; k++) {
            alpha += (alphaTarget - alpha) * alphaDecay;

            for (Force force : forces.values()) {
                force.apply(alpha);
            }

            for (Node node : nodes) {
                if (node.fx == Double.MAX_VALUE) {
                    node.x += node.vx *= velocityDecay;
                } else {
                    node.x = node.fx;
                    node.vx = 0;
                }
                if (node.fy == Double.MAX_VALUE) {
                    node.y += node.vy *= velocityDecay;
                } else {
                    node.y = node.fy;
                    node.vy = 0;
                }
            }
        }
    }

    private void initializeNodes() {
        for (int i = 0; i < nodes.length; i++) {
            Node node = nodes[i];
            node.index = i;
            if (node.fx != Double.MAX_VALUE) {
                node.x = node.fx;
            }
            if (node.fy != Double.MAX_VALUE) {
                node.y = node.fy;
            }
            if (node.fx == Double.MAX_VALUE || node.fy == Double.MAX_VALUE) {
                double radius = initialRadius * Math.sqrt(i);
                double angle = i * initialAngle;
                node.x = radius * Math.cos(angle);
                node.y = radius * Math.sin(angle);
            }
        }
    }

    private Simulation(Builder builder) {
        this.initialRadius = builder.initialRadius;
        this.initialAngle = builder.initialAngle;
        this.alpha = builder.alpha;
        this.alphaMin = builder.alphaMin;
        this.alphaDecay = builder.alphaDecay;
        this.alphaTarget = builder.alphaTarget;
        this.velocityDecay = 1 - builder.velocityDecay;

        if (builder.nodes == null) {
            this.nodes = new Node[0];
        } else {
            this.nodes = builder.nodes.toArray(new Node[0]);
        }

        if (builder.links == null) {
            this.links = new Link[0];
        } else {
            this.links = builder.links.toArray(new Link[0]);
        }

        if (builder.forces == null) {
            this.forces = Collections.emptyMap();
        } else {
            this.forces = builder.forces;
        }

        initializeNodes();

        for (Force force : forces.values()) {
            force.initialize(this);
        }
    }

    public static class Builder {

        private double initialRadius;
        private double initialAngle;
        private double alpha;
        private double alphaMin;
        private double alphaDecay;
        private double alphaTarget;
        private double velocityDecay;

        private List<Node> nodes;
        private List<Link> links;
        private ArrayMap<String, Force> forces;

        public Builder() {
            this.initialRadius = INITIAL_RADIUS;
            this.initialAngle = INITIAL_ANGLE;
            this.alpha = 1;
            this.alphaMin = 0.001;
            this.alphaDecay = 1 - Math.pow(alphaMin, 1.0 / 300);
            this.alphaTarget = 0;
            this.velocityDecay = 0.3;

            this.nodes(null);
            this.links(null);
        }

        public Builder nodes(List<Node> nodes) {
            this.nodes = nodes;
            return this;
        }

        public Builder links(List<Link> links) {
            this.links = links;
            return this;
        }

        public Builder force(String name, Force force) {
            if (name == null || force == null) {
                return this;
            }
            if (forces == null) {
                forces = new ArrayMap<>();
            }
            forces.put(name, force);
            return this;
        }

        public Builder initialRadius(double radius) {
            if (radius > 0) {
                this.initialRadius = radius;
            }
            return this;
        }

        public Builder initialAngle(double angle) {
            if (angle > 0) {
                this.initialAngle = angle;
            }
            return this;
        }

        public Simulation build() {
            return new Simulation(this);
        }
    }
}
