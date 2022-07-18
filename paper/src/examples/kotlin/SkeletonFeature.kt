object SkeletonFeature : Keyed {
    override val id get() = "feature_id"

    class Type : PaperFeature {
        override val id get() = SkeletonFeature.id

        override val statTypes: Map<Key, Stat<*>> get() = emptyMap()
        override val ruleTypes: Map<Key, Class<Rule>> get() = emptyMap()

        override fun createProfile(node: ConfigurationNode) = Profile()

        inner class Profile : PaperFeature.Profile {
            override val type get() = this@Type

            override fun createData() = Data()

            override fun createData(node: ConfigurationNode) = Data()

            override fun createData(tag: CompoundBinaryTag) = Data()

            inner class Data : PaperFeature.Data {
                override val profile get() = this@Profile
                override val type get() = this@Type

                override fun createState() = State()

                override fun serialize(node: ConfigurationNode) {}

                override fun serialize(tag: CompoundBinaryTag.Mutable) {}

                override fun copy() = Data()
            }

            inner class State : PaperFeature.State {
                override val profile get() = this@Profile
                override val type get() = this@Type

                override fun asData() = Data()

                override fun onEvent(event: NodeEvent, ctx: PaperFeatureContext) {}

                override fun serialize(tag: CompoundBinaryTag.Mutable) {}
            }
        }
    }
}
