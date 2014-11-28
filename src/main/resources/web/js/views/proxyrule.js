window.ProxyruleView = Backbone.View.extend({

    initialize:function () {
        this.template = _.template(tpl.get('proxyrule'));
        this.model.bind("change", this.render, this);
        this.model.bind("add", this.render, this);
    },

    render:function (eventName) {
        $(this.el).html(this.template(this.model.toJSON()));
        return this;
    }

});

window.ProxyruleListView = Backbone.View.extend({

    initialize:function () {
        var self = this;
        this.template = _.template(tpl.get('proxyrule-list'));
        this.model.bind("change", this.render, this);
        this.model.bind("add", this.render, this);
        this.model.bind("remove", this.render, this);
    },

    render:function (eventName) {
        $(this.el).html(this.template({nproxyrules:prl.length}));
        _.each(this.model.models, function (pr) {
            $(this.el).find('table.proxyrule-table > tbody')
                .append(new ProxyruleListItemView({model:pr}).render().el);
        }, this);
        return this;
    }
});

window.ProxyruleListItemView = Backbone.View.extend({

    tagName:"tr",

    initialize:function () {
        this.template = _.template(tpl.get('proxyrule-list-item'));
    },

    render:function (eventName) {
        $(this.el).html(this.template(this.model.toJSON()));
        this.model.bind("change", this.render, this);
        this.model.bind("destroy", this.close, this);
        return this;
    }

});