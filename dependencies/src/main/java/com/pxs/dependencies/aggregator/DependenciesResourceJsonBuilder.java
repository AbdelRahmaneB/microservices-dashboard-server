package com.pxs.dependencies.aggregator;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pxs.dependencies.model.Node;

@Component
public class DependenciesResourceJsonBuilder {

	@Autowired
	private HealthIndicatorsAggregator healthIndicatorsAggregator;

	public String build() {
		return healthIndicatorsAggregator.fetchCombinedDependencies();
	}
}
