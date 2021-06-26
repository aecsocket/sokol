# Sokol

Platform-agnostic, data-driven item framework

---

Platforms:
* Paper

## Setup

### Dependencies

* [Java >=16](https://adoptopenjdk.net/?variant=openjdk16&jvmVariant=hotspot)

### Coordinates

Repository
```xml
<repository>
    <id>gitlab-maven-sokol</id>
    <url>https://gitlab.com/api/v4/projects/27149151/packages/maven</url>
</repository>
```

Dependency
```xml
<dependency>
    <groupId>com.gitlab.aecsocket.sokol</groupId>
    <artifactId>sokol-[MODULE]</artifactId>
    <version>[VERSION]</version>
</dependency>
```

Modules:
* `core` Core classes, such as components and systems
* `paper` [Paper >=1.16.5](https://papermc.io/) platform implementation

### API

## Paper

### Dependencies

* Core dependencies
* [Paper >=1.16.5](https://papermc.io/)
* [ProtocolLib >=4.6.0 Dev](https://ci.dmulloy2.net/job/ProtocolLib/lastSuccessfulBuild/)

### [Download](https://gitlab.com/aecsocket/sokol/-/jobs/artifacts/master/raw/sokol-paper/target/Sokol-Paper.jar?job=build)

### Documentation

TODO

### API

The main way to interact with the API is by registering a custom system type, and placing that on a
component in your configuration. Your system will be able to react to component and item events.

Example system implementation:

```java
public class TestSystem extends AbstractSystem implements PaperSystem {
    public static final String ID = "test_system";
    public static final PaperSystem.Type TYPE = (plugin, node) -> new TestSystem(plugin);

    public final class Instance extends AbstractSystem.Instance implements PaperSystem.Instance {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public @NotNull TestSystem base() { return TestSystem.this; }
        @Override public @NotNull SokolPlugin platform() { return platform; }

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

    @Override public @NotNull String id() { return ID; }

    public SokolPlugin platform() { return platform; }

    @Override
    public @NotNull Instance create(TreeNode node, Component component) {
        return new Instance(node);
    }

    @Override
    public @NotNull Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode config) throws SerializationException {
        return new Instance(node);
    }

    @Override
    public @NotNull Instance load(PaperTreeNode node, PersistentDataContainer data) throws IllegalArgumentException {
        return new Instance(node);
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
