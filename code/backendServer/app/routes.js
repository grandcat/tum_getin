var userSchema = new mongoose.Schema({
         tum_id: { type: String, trim: true },
         token: { type: String, trim: true },
         created: { type: Date, default: Date.now },
         lastAccess: { type: Date, default: Date.now },
     },
     { collection: 'user' }
);

userSchema.index({tum_id : 1}, {unique:true});
userSchema.index({token : 1}, {unique:true});

var UserModel = mongoose.model( 'User', userSchema );


