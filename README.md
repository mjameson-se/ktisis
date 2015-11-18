# ktisis
Template &amp; plugin based code generation

At a high level, this project's goal is to facilitate a simple melding of Plugins (code) and Templates (text) to generate source code from a structured definition.
If that sounds like a lot of vaguery, its for a good reason. I have at least one very specific use case in mind for this framework: generating immutable Java POJOs from JSON definition files; it would also be nice to use the same JSON definitions to generate equivalent python source, but that is a less immediate concern. Philosophically I'm convinced that writing extensible code that is as generic as practical is the best way to make forward progress. Towards that end, the Ktisis framework is radically extensible, with core functionality implemented as plugins rather than having a plugin architecture welded on top of core functionality.
So what does it do, you ask?
Simple: it processes each line of a text input stream to see if any of its registered plugins are interested in transforming it. All transformations and processing are contextual, with context in k->v mappings provided by the caller.
The simplest plugin is the substitution plugin, which takes simple expressions like "My name is ${name}" and transforming it to "My name is Michael", when I provide the context mapping name -> "Michael".
The simple beauty of the design is that if you have hundreds of contexts (like POJO definitions), the same configuration of plugins and templates (or a small set of them) can be used to generate source code.
My hope is to eventually write a gradle plugin to fulfill my use case, and perhaps others will find it useful as well. Perhaps instead others will write an Eclipse plugin to generate code from a GUI form. With Ktisis, the possibilities are endless.
