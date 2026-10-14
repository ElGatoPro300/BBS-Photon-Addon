package elgatopro300.bbsphoton.forms;

import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueList;

import java.util.function.Function;

/** Uses the host list contract for persistence, copying, notifications and track paths. */
public class PhotonValueList<T extends BaseValue> extends ValueList<T>
{
    private final Function<String, T> factory;

    public PhotonValueList(String id, Function<String, T> factory)
    {
        super(id);
        this.factory = factory;
    }

    public T append()
    {
        this.preNotify();
        T entry = this.create(Integer.toString(this.list.size()));
        this.add(entry);
        this.sync();
        this.postNotify();
        return entry;
    }

    public void delete(T entry)
    {
        this.preNotify();
        this.list.remove(entry);
        this.sync();
        this.postNotify();
    }

    @Override
    protected T create(String id)
    {
        return this.factory.apply(id);
    }

    @Override
    public void copy(BaseValueGroup group)
    {
        this.list.clear();
        for (BaseValue value : group.getAll())
        {
            T entry = this.create(value.getId());
            this.add(entry);
            entry.copy(value);
        }
        this.sync();
    }
}
