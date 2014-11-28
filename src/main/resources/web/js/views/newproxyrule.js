window.NewProxyruleView = Backbone.View.extend({

    initialize:function () {
        var self = this;
        this.template = _.template(tpl.get('host-list'));
        this.model.bind("change", this.render, this);
        this.model.bind("add", this.render, this);
        this.model.bind("remove", this.render, this);
    },

    render:function (eventName) {
        $(this.el).html(this.template({nhosts:hl.length}));
        _.each(this.model.models, function (h) {
            $(this.el).find('table.host-table > tbody')
                .append(new HostListItemView({model:h}).render().el);
        }, this);
        return this;
    }
});