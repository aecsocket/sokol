# Sokol

Platform-agnostic data-driven item framework, allowing server owners to configure items
and plugin developers to register custom item behaviour.

---

## Paper

Sokol is exposed as a Paper plugin. It has its own configurations and allows other plugins
to depend on it.

### Dependencies

* [Java >=16](https://adoptopenjdk.net/?variant=openjdk16&jvmVariant=hotspot)
* [Paper >=1.17.1](https://papermc.io/)
* [Minecommons >=1.2](https://gitlab.com/aecsocket/minecommons)
* [ProtocolLib >=4.7.0](https://www.spigotmc.org/resources/protocollib.1997/)

### [Download](https://gitlab.com/api/v4/projects/27149151/jobs/artifacts/master/raw/paper/build/libs/sokol-paper-1.2.jar?job=build)

### Documentation

TODO

## Development Setup

### Coordinates

#### Maven

Repository
```xml
<repository>
    <id>gitlab-sokol-minecommons</id>
    <url>https://gitlab.com/api/v4/projects/27149151/packages/maven</url>
</repository>
```
Dependency
```xml
<dependency>
    <groupId>com.gitlab.aecsocket.sokol</groupId>
    <artifactId>[MODULE]</artifactId>
    <version>[VERSION]</version>
</dependency>
```

#### Gradle

Repository
```kotlin
maven("https://gitlab.com/api/v4/projects/27149151/packages/maven")
```

Dependency
```kotlin
implementation("com.gitlab.aecsocket.sokol", "[MODULE]", "[VERSION]")
```

### Usage

#### [Javadoc](https://aecsocket.gitlab.io/sokol)

#### API

The main way to interact with the API is by registering a custom system type, and placing that on a
component in your configuration. Your system will be able to react to component and item events.

Example system implementation:

```java
public class TestSystem extends AbstractSystem implements PaperSystem {
    public static final String ID = "test_system";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);

    public final class Instance extends AbstractSystem.Instance implements PaperSystem.Instance {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public TestSystem base() { return TestSystem.this; }
        @Override public SokolPlugin platform() { return platform; }

        @Override
        public void build() {
            parent.events().register(ItemSystem.Events.CreateItem.class, this::event);
        }

        private void event(ItemSystem.Events.CreateItem event) {
            if (!parent.isRoot())
                return;
            System.out.println("System set up");
        }
    }

    private final SokolPlugin platform;

    public TestSystem(SokolPlugin platform) {
        this.platform = platform;
    }

    @Override public String id() { return ID; }

    public SokolPlugin platform() { return platform; }

    @Override
    public Instance create(TreeNode node) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode config) throws SerializationException {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, PersistentDataContainer data) throws IllegalArgumentException {
        return new Instance(node);
    }
}
```

To register this system type in your plugin, you must:

* 1. Add `Sokol` as a dependency in your `plugin.yml`:

```yaml
name: "MySokolExtension"
# ...
depend:
  # ...
  - "Sokol"
```

* 2. Write some code in your `#onEnable`:

```java
public void onEnable() {
    SokolPlugin sokol = SokolPlugin.instance();
    sokol.registerSystemType(TestSystem.ID, TestSystem.TYPE);
}
```

* 3. If you need to register custom Configurate `TypeSerializer`s before loading the configuration:

```java
sokol.configOptionInitializer((serializers, mapperFactory) -> serializers
        .register(MyCustomType.class, new MyCustomTypeSerializer()));
```


### Modules

* Core `core`

Implementations:
* Paper `paper`
