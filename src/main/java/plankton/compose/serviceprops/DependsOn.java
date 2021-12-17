package plankton.compose.serviceprops;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import plankton.compose.DependsOnCondition;
import plankton.compose.ServiceProperty;

public class DependsOn extends ServiceProperty<DependsOn> {

    private final Map<String, DependsOnCondition> serviceConditionMap = new HashMap<>();
    // TODO it should be Map<ComposeService, DependsOnCondition>

    public DependsOn() {
        super("depends_on");
    }

    @Override
    public void initialize(Object object) {
        if (object instanceof String) {
            serviceConditionMap.put((String) object, DependsOnCondition.SERVICE_COMPLETED_SUCCESSFULLY);
        } else if (object instanceof List) {
            List<String> list = castToStringList(object);
            list.forEach(serviceName -> serviceConditionMap.put(serviceName,
                    DependsOnCondition.SERVICE_COMPLETED_SUCCESSFULLY));
        } else if (object instanceof Map) {
            Map<String, Map<String, Object>> m = castToMapOfMaps(object);
            m.forEach((serviceName, serviceMap) -> {
                String conditionString = (String) serviceMap.remove("condition");
                DependsOnCondition condition = DependsOnCondition.of(conditionString);
                serviceConditionMap.put(serviceName, condition);
                serviceMap.keySet().forEach(k -> ignoredKeys.add(serviceName + "." + k));
            });
        }
    }

    @Override
    public DependsOn applyTo(DependsOn o) {
        DependsOn other = (o == null) ? new DependsOn() : o;
        this.serviceConditionMap.forEach((k, v) -> {
            if (!other.serviceConditionMap.containsKey(k))
                other.serviceConditionMap.put(k, v);
        });
        return other;
    }

    public Map<String, DependsOnCondition> serviceConditionMap() {
        return Collections.unmodifiableMap(serviceConditionMap);
    }

    @Override
    public String toString() {
        return serviceConditionMap.toString();
    }
}
