package com.traffic.simulator.service;

import com.traffic.simulator.entity.EdgeEntity;
import com.traffic.simulator.entity.NodeEntity;
import com.traffic.simulator.repository.EdgeRepository;
import com.traffic.simulator.repository.NodeRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class DataLoader implements CommandLineRunner {

    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;

    public DataLoader(NodeRepository nodeRepository, EdgeRepository edgeRepository) {
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (nodeRepository.count() > 0 || edgeRepository.count() > 0) {
            return;
        }

        NodeEntity mumbai = new NodeEntity(null, "Mumbai", 19.0760, 72.8777);
        NodeEntity pune = new NodeEntity(null, "Pune", 18.5204, 73.8567);
        NodeEntity nashik = new NodeEntity(null, "Nashik", 19.9975, 73.7898);
        NodeEntity nagpur = new NodeEntity(null, "Nagpur", 21.1458, 79.0882);
        NodeEntity aurangabad = new NodeEntity(null, "Aurangabad", 19.8762, 75.3433);
        NodeEntity kolhapur = new NodeEntity(null, "Kolhapur", 16.7049, 74.2433);
        NodeEntity solapur = new NodeEntity(null, "Solapur", 17.6599, 75.9064);
        NodeEntity surat = new NodeEntity(null, "Surat", 21.1702, 72.8311);
        NodeEntity ahmedabad = new NodeEntity(null, "Ahmedabad", 23.0225, 72.5714);
        NodeEntity goa = new NodeEntity(null, "Goa", 15.2993, 74.1240);

        List<NodeEntity> nodes = nodeRepository.saveAll(List.of(
                mumbai, pune, nashik, nagpur, aurangabad, kolhapur, solapur, surat, ahmedabad, goa));

        mumbai = nodes.get(0);
        pune = nodes.get(1);
        nashik = nodes.get(2);
        nagpur = nodes.get(3);
        aurangabad = nodes.get(4);
        kolhapur = nodes.get(5);
        solapur = nodes.get(6);
        surat = nodes.get(7);
        ahmedabad = nodes.get(8);
        goa = nodes.get(9);

        edgeRepository.saveAll(List.of(
                edge(mumbai, pune, 150.0, 1.1),
                edge(mumbai, nashik, 170.0, 1.2),
                edge(mumbai, surat, 280.0, 1.15),
                edge(pune, aurangabad, 235.0, 1.25),
                edge(pune, kolhapur, 235.0, 1.1),
                edge(nashik, aurangabad, 175.0, 1.2),
                edge(aurangabad, nagpur, 500.0, 1.35),
                edge(solapur, kolhapur, 240.0, 1.15),
                edge(surat, ahmedabad, 265.0, 1.1),
                edge(ahmedabad, nashik, 440.0, 1.3),
                edge(goa, kolhapur, 190.0, 1.2),
                edge(goa, pune, 440.0, 1.35),
                edge(solapur, aurangabad, 260.0, 1.2),
                edge(nagpur, surat, 730.0, 1.4)
        ));
    }

    private EdgeEntity edge(NodeEntity from, NodeEntity to, double distanceKm, double congestionWeight) {
        EdgeEntity edge = new EdgeEntity();
        edge.setFromNode(from);
        edge.setToNode(to);
        edge.setDistanceKm(distanceKm);
        edge.setCongestionWeight(congestionWeight);
        return edge;
    }
}
